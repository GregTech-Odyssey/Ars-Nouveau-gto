package com.hollingsworth.arsnouveau.common.compat.ae2;

import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.hollingsworth.arsnouveau.common.entity.EntityFlyingItem;
import com.hollingsworth.arsnouveau.common.network.ClientFx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.MEStorageHost;
import appeng.api.storage.StorageAccess;
import appeng.api.storage.StorageTargetResolver;

public final class ScribesNetworkSource {

    private ScribesNetworkSource() {}

    public static void takeFromNetwork(ScribesTile scribes) {
        Level level = scribes.getLevel();
        if (level == null) return;
        BlockPos center = scribes.getBlockPos();
        StorageTargetResolver resolver = null;
        for (BlockPos pos : BlockPos.betweenClosed(center.north(6).east(6).below(2), center.south(6).west(6).above(2))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) continue;
            MEStorage storage;
            if (be instanceof MEStorageHost host) {
                storage = host.getAnyMEStorage();
            } else {
                if (resolver == null) resolver = new StorageTargetResolver();
                storage = anyStorage(resolver, be);
            }
            if (storage != null) {
                extract(scribes, level, storage, pos);
                return;
            }
        }
    }

    private static @Nullable MEStorage anyStorage(StorageTargetResolver resolver, BlockEntity be) {
        if (resolver.resolveAll(be, null, StorageAccess.FULL) == StorageTargetResolver.Tier.STORAGE) return (MEStorage) resolver.raw();
        for (Direction side : Direction.values()) {
            if (resolver.resolveAll(be, side, StorageAccess.FULL) == StorageTargetResolver.Tier.STORAGE) return (MEStorage) resolver.raw();
        }
        return null;
    }

    private static void extract(ScribesTile scribes, Level level, MEStorage storage, BlockPos pos) {
        for (var stored : storage.getAvailableStacks()) {
            if (stored.getKey() instanceof AEItemKey item && scribes.canConsumeItemstack(item.wrapForDisplayOrFilter())) {
                long extracted = storage.extract(item, 1, Actionable.MODULATE, IActionSource.empty());
                ItemStack taken = item.toStack((int) extracted);
                scribes.consumedStacks.add(taken);
                EntityFlyingItem flyingItem = new EntityFlyingItem(level, pos, scribes.getBlockPos());
                flyingItem.setStack(taken);
                ClientFx.spawn(level, flyingItem);
                scribes.updateBlock();
                return;
            }
        }
    }
}
