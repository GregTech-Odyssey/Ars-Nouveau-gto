package com.hollingsworth.arsnouveau.common.compat.ae2;

import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;

import org.jetbrains.annotations.Nullable;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.core.localization.GuiText;
import appeng.util.BlockApiCache;

import com.hollingsworth.arsnouveau.api.source.IAdvancedSourceTile;
import com.hollingsworth.arsnouveau.api.source.SourceTileWrapper;

import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class SourceExternalStorageStrategy implements ExternalStorageStrategy {
    private final BlockApiCache<IAdvancedSourceTile> apiCache;
    private final Direction fromSide;
    private final ServerLevel level;
    private final BlockPos busPos;

    public SourceExternalStorageStrategy(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        apiCache = BlockApiCache.create(CapabilityRegistry.SOURCE_TILE, level, fromPos);
        this.fromSide = fromSide;
        this.level = level;
        this.busPos = fromPos.relative(fromSide);
    }

    @Nullable
    @Override
    public MEStorage createWrapper(boolean extractableOnly, Runnable injectOrExtractCallback) {
        var sourceTile = apiCache.find(fromSide);
        return sourceTile != null ? new Adaptor(sourceTile, injectOrExtractCallback, this::busGrid) : null;
    }

    @Nullable
    private IGrid busGrid() {
        var host = GridHelper.getNodeHost(level, busPos);
        var node = host == null ? null : host.getGridNode(fromSide.getOpposite());
        return node == null ? null : node.getGrid();
    }

    private record Adaptor(ISourceTile sourceTile, Runnable injectOrExtractCallback, Supplier<IGrid> busGrid) implements MEStorage {
        private boolean ownNetwork() {
            return unwrap(sourceTile) instanceof MESourceJarBlockEntity jar && jar.isOnGrid(busGrid.get());
        }

        private static ISourceTile unwrap(ISourceTile tile) {
            return tile instanceof SourceTileWrapper wrapper ? wrapper.tile() : tile;
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource actionSource) {
            if (!(what instanceof SourceKey) || ownNetwork()) {
                return 0;
            }

            long inserted = sourceTile.addSource(amount, mode == Actionable.SIMULATE);

            if (inserted > 0 && mode == Actionable.MODULATE) {
                injectOrExtractCallback.run();
            }

            return inserted;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource actionSource) {
            if (!(what instanceof SourceKey) || ownNetwork()) {
                return 0;
            }

            long extracted = sourceTile.removeSource(amount, mode == Actionable.SIMULATE);

            if (extracted > 0 && mode == Actionable.MODULATE) {
                injectOrExtractCallback.run();
            }

            return extracted;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            if (ownNetwork()) {
                return;
            }
            var currentSource = sourceTile.getSource();

            if (currentSource != 0) {
                out.add(SourceKey.KEY, currentSource);
            }
        }

        @Override
        public Component getDescription() {
            return GuiText.ExternalStorage.text(SourceKeyType.TYPE.getDescription());
        }
    }
}
