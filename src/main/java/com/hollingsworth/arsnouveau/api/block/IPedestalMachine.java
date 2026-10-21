package com.hollingsworth.arsnouveau.api.block;

import com.hollingsworth.arsnouveau.api.util.ArsBlockEntityIndexes;
import com.suried.gtoutils.spatial.BlockOrder;
import com.suried.gtoutils.spatial.BlockRange;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface IPedestalMachine {
    default List<BlockPos> pedestalList(BlockPos blockPos, int offset, @NotNull Level level) {
        ArrayList<BlockPos> posList = new ArrayList<>();
        ArsBlockEntityIndexes.PEDESTALS.forEach(level, blockPos, BlockRange.box(offset), null, tile -> posList.add(tile.getBlockPos()));
        BlockOrder.sortBetweenClosed(posList, pos -> pos);
        return posList;
    }

    void lightPedestal(Level level);
}
