package com.hollingsworth.arsnouveau.api.source;

import com.hollingsworth.arsnouveau.api.util.ArsBlockEntityIndexes;
import com.hollingsworth.arsnouveau.common.block.tile.SourcelinkTile;
import com.suried.gtoutils.spatial.BlockRange;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

public class SourcelinkEventQueue {

    public static void addManaEvent(Level world, Class<? extends SourcelinkTile> tileType, int amount, Event event, BlockPos sourcePos) {
        addManaEvent(world, tileType, (long) amount, event, sourcePos);
    }

    public static void addManaEvent(Level world, Class<? extends SourcelinkTile> tileType, long amount, Event event, BlockPos sourcePos) {
        SourcelinkTile tile = ArsBlockEntityIndexes.EVENT_SOURCELINKS.findNearest(world, sourcePos, BlockRange.sphere(SourcelinkTile.EVENT_RANGE),
                t -> t.getClass().equals(tileType) && !t.isRemoved() && t.eventInRange(sourcePos, event) && t.canAcceptSource());
        if (tile != null) {
            tile.getManaEvent(sourcePos, amount);
        }
    }
}
