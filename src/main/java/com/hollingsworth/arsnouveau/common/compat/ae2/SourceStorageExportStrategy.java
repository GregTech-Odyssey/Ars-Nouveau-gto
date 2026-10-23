package com.hollingsworth.arsnouveau.common.compat.ae2;

import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import appeng.api.behaviors.StackExportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import appeng.util.BlockApiCache;

import com.hollingsworth.arsnouveau.api.source.IAdvancedSourceTile;

@SuppressWarnings("UnstableApiUsage")
public class SourceStorageExportStrategy implements StackExportStrategy {
    private final BlockApiCache<IAdvancedSourceTile> apiCache;
    private final Direction fromSide;

    public SourceStorageExportStrategy(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        apiCache = BlockApiCache.create(CapabilityRegistry.SOURCE_TILE, level, fromPos);
        this.fromSide = fromSide;
    }

    @Override
    public long transfer(StackTransferContext context, AEKey what, long amount) {
        if (!(what instanceof SourceKey)) {
            return 0;
        }

        var sourceTile = apiCache.find(fromSide);

        if (sourceTile != null) {
            var insertable = sourceTile.addSource(amount, true);
            if (insertable <= 0) {
                return 0;
            }
            var inventory = context.getInternalStorage().getInventory();
            var extracted = StorageHelper.poweredExtraction(
                    context.getEnergySource(),
                    inventory,
                    SourceKey.KEY,
                    insertable,
                    context.getActionSource(),
                    Actionable.MODULATE);
            if (extracted <= 0) {
                return 0;
            }
            var given = sourceTile.addSource(extracted, false);
            if (given < extracted) {
                inventory.insert(SourceKey.KEY, extracted - given, Actionable.MODULATE, context.getActionSource());
            }

            return given;
        }

        return 0;
    }

    @Override
    public long push(AEKey what, long amount, Actionable mode) {
        if (!(what instanceof SourceKey)) {
            return 0;
        }

        var sourceTile = apiCache.find(fromSide);

        if (sourceTile != null) {
            return sourceTile.addSource(amount, mode == Actionable.SIMULATE);
        }

        return 0;
    }
}
