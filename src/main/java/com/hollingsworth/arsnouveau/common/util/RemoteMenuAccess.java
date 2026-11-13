package com.hollingsworth.arsnouveau.common.util;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.common.network.Networking;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Menus a player opens through the Interact glyph stay valid away from the block: distances are measured from the block,
 * and a chunk outside the player's view is sent to that client alone while the menu is open.
 */
@Mod.EventBusSubscriber(modid = ArsNouveau.MODID)
public final class RemoteMenuAccess {

    private static Entity viewer;
    private static double originX, originY, originZ;

    private RemoteMenuAccess() {}

    public record Origin(ResourceKey<Level> dimension, BlockPos pos, boolean chunkSent) {}

    public interface Holder {

        @Nullable
        Origin ars_nouveau$remoteOrigin();

        void ars_nouveau$setRemoteOrigin(@Nullable Origin origin);
    }

    public static void interact(Player player, Level level, BlockPos pos, Runnable interaction) {
        if (!(player instanceof ServerPlayer serverPlayer) || player instanceof FakePlayer || !(level instanceof ServerLevel serverLevel)) {
            interaction.run();
            return;
        }
        AbstractContainerMenu before = serverPlayer.containerMenu;
        boolean chunkSent = sendChunk(serverPlayer, serverLevel, pos);
        interaction.run();
        AbstractContainerMenu opened = serverPlayer.containerMenu;
        if (opened != before && opened != serverPlayer.inventoryMenu) {
            ((Holder) opened).ars_nouveau$setRemoteOrigin(new Origin(level.dimension(), pos.immutable(), chunkSent));
        } else if (chunkSent) {
            releaseChunk(serverPlayer, serverLevel, pos);
        }
    }

    public static boolean stillValid(AbstractContainerMenu menu, Player player, Operation<Boolean> check) {
        Origin origin = ((Holder) menu).ars_nouveau$remoteOrigin();
        if (origin == null) return check.call(menu, player);
        Level level = player.level();
        BlockPos pos = origin.pos();
        if (level.dimension() != origin.dimension() || !level.isLoaded(pos)) return false;
        Entity previous = viewer;
        double px = originX, py = originY, pz = originZ;
        viewer = player;
        originX = pos.getX() + 0.5;
        originY = pos.getY() + 0.5;
        originZ = pos.getZ() + 0.5;
        try {
            return check.call(menu, player);
        } finally {
            viewer = previous;
            originX = px;
            originY = py;
            originZ = pz;
        }
    }

    public static boolean isViewer(Entity entity) {
        return viewer == entity;
    }

    public static double originDistanceSqr(double x, double y, double z) {
        double dx = originX - x;
        double dy = originY - y;
        double dz = originZ - z;
        return dx * dx + dy * dy + dz * dz;
    }

    @SubscribeEvent
    public static void onMenuClosed(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Origin origin = ((Holder) event.getContainer()).ars_nouveau$remoteOrigin();
        if (origin == null || !origin.chunkSent() || player.level().dimension() != origin.dimension()) return;
        releaseChunk(player, player.serverLevel(), origin.pos());
    }

    private static boolean sendChunk(ServerPlayer player, ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
        if (chunk == null || chunk.getBlockEntity(pos) == null || isTracking(player, level, chunkPos)) return false;
        Networking.sendToPlayerClient(new ChunkPacket(chunkPos.x, chunkPos.z, ChunkPacket.EXPECT), player);
        player.connection.send(new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null));
        return true;
    }

    private static void releaseChunk(ServerPlayer player, ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        byte action = isTracking(player, level, chunkPos) ? ChunkPacket.RELEASE : ChunkPacket.RELEASE_AND_FORGET;
        Networking.sendToPlayerClient(new ChunkPacket(chunkPos.x, chunkPos.z, action), player);
    }

    private static boolean isTracking(ServerPlayer player, ServerLevel level, ChunkPos pos) {
        return level.getChunkSource().chunkMap.getPlayers(pos, false).contains(player);
    }

    public record ChunkPacket(int x, int z, byte action) {

        static final byte EXPECT = 0, RELEASE = 1, RELEASE_AND_FORGET = 2;

        public static void encode(ChunkPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.x);
            buf.writeInt(msg.z);
            buf.writeByte(msg.action);
        }

        public static ChunkPacket decode(FriendlyByteBuf buf) {
            return new ChunkPacket(buf.readInt(), buf.readInt(), buf.readByte());
        }

        public static void handle(ChunkPacket msg, Supplier<NetworkEvent.Context> ctx) {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                ctx.get().enqueueWork(new Runnable() {
                    @Override
                    public void run() {
                        Client.handle(msg);
                    }
                });
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class Client {

        private static final long NONE = ChunkPos.INVALID_CHUNK_POS;
        private static long expected = NONE;
        private static LevelChunk chunk;

        private Client() {}

        static void handle(ChunkPacket msg) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;
            long key = ChunkPos.asLong(msg.x(), msg.z());
            if (msg.action() == ChunkPacket.EXPECT) {
                release(level);
                expected = key;
                return;
            }
            if (expected == key) expected = NONE;
            if (holds(msg.x(), msg.z())) release(level);
            else if (msg.action() == ChunkPacket.RELEASE_AND_FORGET) level.getChunkSource().drop(msg.x(), msg.z());
        }

        public static boolean expects(int x, int z) {
            return expected == ChunkPos.asLong(x, z);
        }

        public static LevelChunk load(ClientLevel level, int x, int z, FriendlyByteBuf buffer, CompoundTag heightmaps,
                                      Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> blockEntities) {
            release(level);
            ChunkPos pos = new ChunkPos(x, z);
            LevelChunk loaded = new LevelChunk(level, pos);
            loaded.replaceWithPacketData(buffer, heightmaps, blockEntities);
            chunk = loaded;
            level.onChunkLoaded(pos);
            MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(loaded, false));
            return loaded;
        }

        public static LevelChunk orHeld(LevelChunk original, int x, int z) {
            return holds(x, z) ? chunk : original;
        }

        public static boolean holding() {
            return chunk != null;
        }

        public static void onVanillaLoad(int x, int z) {
            if (!holds(x, z)) return;
            LevelChunk stale = chunk;
            chunk = null;
            MinecraftForge.EVENT_BUS.post(new ChunkEvent.Unload(stale));
            stale.clearAllBlockEntities();
        }

        public static void onDrop(ClientLevel level, int x, int z) {
            if (holds(x, z)) release(level);
        }

        public static void reset() {
            chunk = null;
            expected = NONE;
        }

        private static boolean holds(int x, int z) {
            LevelChunk held = chunk;
            return held != null && held.getPos().x == x && held.getPos().z == z;
        }

        private static void release(ClientLevel level) {
            LevelChunk held = chunk;
            if (held == null) return;
            chunk = null;
            MinecraftForge.EVENT_BUS.post(new ChunkEvent.Unload(held));
            level.unload(held);
        }
    }
}
