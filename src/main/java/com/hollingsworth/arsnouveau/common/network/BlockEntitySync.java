package com.hollingsworth.arsnouveau.common.network;

import com.gto.datasynclib.IFieldDataHolder;
import com.gto.datasynclib.LazyFieldDataManager;
import com.gto.datasynclib.LogicalSide;
import com.gto.datasynclib.network.BlockEntitySyncPacket;
import com.gto.datasynclib.network.DataSyncNetwork;
import com.gto.datasynclib.util.ByteBufCodecExtends;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Server to client block entity sync over DataSyncLib: requests merge per level tick, are skipped while nobody tracks the chunk,
 * and only the top-level NBT keys that changed since the last send go over the wire.
 */
public final class BlockEntitySync {
    private static final String FULL_KEY = "ars_sync";
    private static final byte NONE = 0;
    private static final byte FULL = 1;
    private static final byte DELTA = 2;
    private static final int MAX_KEYS = 4096;
    private static final ReferenceLinkedOpenHashSet<BlockEntity> PENDING = new ReferenceLinkedOpenHashSet<>();

    public interface Holder extends IFieldDataHolder {
        BlockEntitySync ars$sync();
    }

    private final Holder owner;
    private final LazyFieldDataManager manager;
    private final Supplier<CompoundTag> tagWriter;
    private final Consumer<CompoundTag> tagReader;
    private CompoundTag baseline;
    private CompoundTag changed;
    private List<String> removed;

    public BlockEntitySync(Holder owner, Supplier<CompoundTag> tagWriter, Consumer<CompoundTag> tagReader) {
        this.owner = owner;
        this.manager = new LazyFieldDataManager(owner);
        this.tagWriter = tagWriter;
        this.tagReader = tagReader;
    }

    public LazyFieldDataManager manager() {
        return manager;
    }

    public static void request(BlockEntity be) {
        if (be.getLevel() instanceof ServerLevel && be instanceof Holder) {
            PENDING.add(be);
        }
    }

    public static void flush(ServerLevel level) {
        if (PENDING.isEmpty()) {
            return;
        }
        List<BlockEntity> due = null;
        var it = PENDING.iterator();
        while (it.hasNext()) {
            var be = it.next();
            if (be.getLevel() == level || be.getLevel() == null) {
                it.remove();
                if (due == null) {
                    due = new ArrayList<>();
                }
                due.add(be);
            }
        }
        if (due != null) {
            for (var be : due) {
                send(be);
            }
        }
    }

    public static void forget(Level level) {
        PENDING.removeIf(be -> be.getLevel() == level || be.getLevel() == null);
    }

    public static void clear() {
        PENDING.clear();
    }

    private static void send(BlockEntity be) {
        if (be.isRemoved() || !(be.getLevel() instanceof ServerLevel level) || !(be instanceof Holder holder)) {
            return;
        }
        var players = level.getChunkSource().chunkMap.getPlayers(new ChunkPos(be.getBlockPos()), false);
        if (players.isEmpty()) {
            return;
        }
        var sync = holder.ars$sync();
        var manager = sync.manager.get();
        if (sync.prepareDelta()) {
            manager.markAsChanged();
        }
        if (!manager.updateFieldDirtyFlags(LogicalSide.SERVER, true)) {
            return;
        }
        byte[] data = manager.writeToNetworkBuffer(LogicalSide.SERVER, false);
        if (data.length > 0) {
            var chunk = level.getChunkAt(be.getBlockPos());
            DataSyncNetwork.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), new BlockEntitySyncPacket(be.getBlockPos(), data));
        }
    }

    public CompoundTag writeFull() {
        var be = (BlockEntity) owner;
        if (PENDING.remove(be)) {
            send(be);
        }
        var tag = new CompoundTag();
        tag.putByteArray(FULL_KEY, manager.get().writeToNetworkBuffer(LogicalSide.SERVER, true));
        return tag;
    }

    public static boolean isFullSync(CompoundTag tag) {
        return tag.get(FULL_KEY) instanceof ByteArrayTag;
    }

    public void readFull(CompoundTag tag) {
        if (tag.get(FULL_KEY) instanceof ByteArrayTag data) {
            manager.get().readFromNetworkBuffer(LogicalSide.CLIENT, data.getAsByteArray());
        }
    }

    private boolean prepareDelta() {
        var current = tagWriter.get();
        var base = baseline;
        if (base == null) {
            baseline = current;
            changed = current;
            removed = null;
            return true;
        }
        CompoundTag diff = null;
        for (String key : current.getAllKeys()) {
            Tag now = current.get(key);
            if (!now.equals(base.get(key))) {
                if (diff == null) {
                    diff = new CompoundTag();
                }
                diff.put(key, now);
            }
        }
        List<String> gone = null;
        for (String key : base.getAllKeys()) {
            if (!current.contains(key)) {
                if (gone == null) {
                    gone = new ArrayList<>();
                }
                gone.add(key);
            }
        }
        changed = diff;
        removed = gone;
        if (diff == null && gone == null) {
            return false;
        }
        baseline = current;
        return true;
    }

    public void writeCustom(FriendlyByteBuf buf, boolean writeAll) {
        if (writeAll) {
            buf.writeByte(FULL);
            ByteBufCodecExtends.COMPOUND_TAG_CODEC.encode(buf, tagWriter.get());
            return;
        }
        var diff = changed;
        var gone = removed;
        changed = null;
        removed = null;
        if (diff == null && gone == null) {
            buf.writeByte(NONE);
            return;
        }
        buf.writeByte(DELTA);
        var keys = diff == null ? List.<String>of() : new ArrayList<>(diff.getAllKeys());
        buf.writeVarInt(keys.size());
        for (String key : keys) {
            buf.writeUtf(key);
            ByteBufCodecExtends.TAG_CODEC.encode(buf, diff.get(key));
        }
        var removedKeys = gone == null ? List.<String>of() : gone;
        buf.writeVarInt(removedKeys.size());
        for (String key : removedKeys) {
            buf.writeUtf(key);
        }
    }

    public void readCustom(FriendlyByteBuf buf) {
        var level = ((BlockEntity) owner).getLevel();
        if (level == null || !level.isClientSide) {
            return;
        }
        byte mode = buf.readByte();
        if (mode == NONE) {
            return;
        }
        CompoundTag merged;
        if (mode == FULL) {
            merged = ByteBufCodecExtends.COMPOUND_TAG_CODEC.decode(buf);
        } else {
            merged = baseline == null ? new CompoundTag() : baseline.copy();
            int count = buf.readVarInt();
            checkCount(count);
            for (int i = 0; i < count; i++) {
                String key = buf.readUtf();
                merged.put(key, ByteBufCodecExtends.TAG_CODEC.decode(buf));
            }
            int gone = buf.readVarInt();
            checkCount(gone);
            for (int i = 0; i < gone; i++) {
                merged.remove(buf.readUtf());
            }
        }
        baseline = merged;
        tagReader.accept(merged.copy());
    }

    private static void checkCount(int count) {
        if (count < 0 || count > MAX_KEYS) {
            throw new IllegalArgumentException("Too many synced keys: " + count);
        }
    }
}
