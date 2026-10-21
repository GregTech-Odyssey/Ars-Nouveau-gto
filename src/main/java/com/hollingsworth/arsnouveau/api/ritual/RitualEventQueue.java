package com.hollingsworth.arsnouveau.api.ritual;

import com.hollingsworth.arsnouveau.api.util.ArsBlockEntityIndexes;
import com.hollingsworth.arsnouveau.common.block.tile.RitualBrazierTile;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class RitualEventQueue {

    @SuppressWarnings("unchecked")
    public static <T extends RangeRitual> List<T> getRituals(Level level, Class<T> type){
        List<T> rituals = new ArrayList<>();
        ArsBlockEntityIndexes.RITUAL_BRAZIERS.forEach(level, tile -> {
            AbstractRitual ritual = tile.ritual;
            if (ritual != null && ritual.isRunning() && ritual.getClass().equals(type)) {
                rituals.add((T) ritual);
            }
        });
        return rituals;
    }

    /**
     * Used to run operations on the first matching ranged ritual.
     * Returns the first ritual of the given type that matches the predicate.
     */
    @SuppressWarnings("unchecked")
    public static @Nullable <T extends RangeRitual> T getRitual(Level level, Class<T> type, Predicate<T> isMatch){
        RitualBrazierTile tile = ArsBlockEntityIndexes.RITUAL_BRAZIERS.findAny(level, brazier -> {
            AbstractRitual ritual = brazier.ritual;
            return ritual != null && ritual.isRunning() && ritual.getClass().equals(type) && isMatch.test((T) ritual);
        });
        return tile == null ? null : (T) tile.ritual;
    }
}
