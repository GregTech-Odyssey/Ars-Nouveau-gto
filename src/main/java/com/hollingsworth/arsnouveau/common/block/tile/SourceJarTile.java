package com.hollingsworth.arsnouveau.common.block.tile;

import com.hollingsworth.arsnouveau.api.client.ITooltipProvider;
import com.hollingsworth.arsnouveau.api.source.AbstractSourceMachine;
import com.hollingsworth.arsnouveau.common.block.ITickable;
import com.hollingsworth.arsnouveau.common.block.SourceJar;
import com.hollingsworth.arsnouveau.common.util.RegistryWrapper;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SourceJarTile extends AbstractSourceMachine implements ITooltipProvider, ITickable {

    public SourceJarTile(BlockPos pos, BlockState state) {
        super(BlockRegistry.SOURCE_JAR_TILE, pos, state);
    }

    public SourceJarTile(BlockEntityType<? extends SourceJarTile> tileTileEntityType, BlockPos pos, BlockState state) {
        super(tileTileEntityType, pos, state);
    }

    public SourceJarTile(RegistryWrapper<? extends BlockEntityType<?>> type, BlockPos pos, BlockState state) {
        super(type.get(), pos, state);
    }

    @Override
    public int getMaxSource() {
        return 10000;
    }

    @Override
    public boolean updateBlock() {
        super.updateBlock();
        if (level == null || level.isClientSide)
            return true;
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(SourceJar.fill)) {
            int fillState = fillLevel(this.getSourceLong(), this.getMaxSourceLong());
            if (state.getValue(SourceJar.fill) != fillState)
                level.setBlock(worldPosition, state.setValue(SourceJar.fill, fillState), 3);
        }
        return true;
    }

    public static int fillLevel(long source, long maxSource) {
        if (source <= 0 || maxSource <= 0)
            return 0;
        return (int) Math.min(11, 1 + source * 10 / maxSource);
    }

    @Override
    public int getTransferRate() {
        return getMaxSource();
    }

    @Override
    public void getTooltip(List<Component> tooltip) {
        long max = this.getMaxSourceLong();
        tooltip.add(Component.translatable("ars_nouveau.source_jar.fullness", max <= 0 ? 0 : this.getSourceLong() * 100 / max));
    }
}
