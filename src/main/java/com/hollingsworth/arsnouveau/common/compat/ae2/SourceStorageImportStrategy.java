package com.hollingsworth.arsnouveau.common.compat.ae2;

import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.util.BlockApiCache;

import com.hollingsworth.arsnouveau.api.source.IAdvancedSourceTile;

@SuppressWarnings("UnstableApiUsage")
public class SourceStorageImportStrategy implements StackImportStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceStorageImportStrategy.class);

    private final BlockApiCache<IAdvancedSourceTile> apiCache;
    private final Direction fromSide;

    public SourceStorageImportStrategy(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        apiCache = BlockApiCache.create(CapabilityRegistry.SOURCE_TILE, level, fromPos);
        this.fromSide = fromSide;
    }

    @Override
    public boolean transfer(StackTransferContext context) {
        if (!context.isKeyTypeEnabled(SourceKeyType.TYPE)) {
            return false;
        }

        var sourceTile = apiCache.find(fromSide);

        if (sourceTile == null) {
            return false;
        }

        int remainingTransferAmount = context.getOperationsRemaining() * SourceKeyType.TYPE.getAmountPerOperation();
        int rawAmount = Math.min(remainingTransferAmount, sourceTile.getSource());

        var inv = context.getInternalStorage().getInventory();

        // Check how much source we can actually insert
        var amount = inv.insert(SourceKey.KEY, rawAmount, Actionable.SIMULATE, context.getActionSource());

        if (amount > 0) {
            amount = sourceTile.removeSource(amount, false);
        }

        var inserted = amount > 0 ? inv.insert(SourceKey.KEY, amount, Actionable.MODULATE, context.getActionSource()) : 0;

        if (inserted < amount) {
            var leftover = amount - inserted;
            var backFill = sourceTile.addSource(leftover, false);

            if (leftover > backFill) {
                LOGGER.error("Storage import issue, voided {} source.", leftover - backFill);
            }
        }

        var opsUsed = Math.max(1, inserted / SourceKeyType.TYPE.getAmountPerOperation());
        context.reduceOperationsRemaining(opsUsed);

        return amount > 0;
    }
}
