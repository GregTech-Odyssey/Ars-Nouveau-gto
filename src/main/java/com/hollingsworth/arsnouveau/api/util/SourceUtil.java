package com.hollingsworth.arsnouveau.api.util;

import com.hollingsworth.arsnouveau.common.network.ClientFx;
import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.source.IAdvancedSourceTile;
import com.hollingsworth.arsnouveau.api.source.SourceProvider;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarSpec;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.common.entity.EntityFollowProjectile;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.suried.gtoutils.spatial.BlockOrder;
import com.suried.gtoutils.spatial.BlockRange;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = ArsNouveau.MODID)
public class SourceUtil {

    public static List<ISpecialSourceProvider> canGiveSource(BlockPos pos, Level world, int range) {
        List<ISpecialSourceProvider> posList = new ArrayList<>();
        for (SourceJarTile jar : jarsAround(world, pos, range, SourceJarTile::canAcceptSource)) {
            posList.add(new SourceProvider(jar, jar.nearestPart(pos)));
        }
        List<ISpecialSourceProvider> found = new ArrayList<>();
        ArsBlockEntityIndexes.SOURCE_TILES.forEach(world, pos, BlockRange.box(range), null, be -> {
            IAdvancedSourceTile tile = sourceTileAt(world, pos, be);
            if (tile != null && tile.canAcceptSource() && tile.sourcelinksCanProvidePower()) {
                found.add(new SourceProvider(tile, be.getBlockPos().immutable()));
            }
        });
        BlockOrder.sortWithinManhattan(found, pos, ISpecialSourceProvider::getCurrentPos);
        posList.addAll(found);
        return posList;
    }

    public static List<ISpecialSourceProvider> canTakeSource(BlockPos pos, Level world, int range) {
        List<ISpecialSourceProvider> posList = new ArrayList<>();
        for (SourceJarTile jar : jarsAround(world, pos, range, jar -> jar.getSourceLong() > 0)) {
            posList.add(new SourceProvider(jar, jar.nearestPart(pos)));
        }
        List<ISpecialSourceProvider> found = new ArrayList<>();
        ArsBlockEntityIndexes.SOURCE_TILES.forEach(world, pos, BlockRange.box(range), null, be -> {
            IAdvancedSourceTile tile = sourceTileAt(world, pos, be);
            if (tile != null && tile.getSource() > 0 && tile.relayCanTakePower()) {
                found.add(new SourceProvider(tile, be.getBlockPos().immutable()));
            }
        });
        BlockOrder.sortWithinManhattan(found, pos, ISpecialSourceProvider::getCurrentPos);
        posList.addAll(found);
        return posList;
    }

    private static List<SourceJarTile> jarsAround(Level level, BlockPos center, int range, Predicate<? super SourceJarTile> filter) {
        if (ArsBlockEntityIndexes.SOURCE_JARS.size(level) == 0) return Collections.emptyList();
        List<SourceJarTile> found = new ArrayList<>();
        ArsBlockEntityIndexes.SOURCE_JARS.query(level, center, BlockRange.box(range + SourceJarSpec.maxReach()), jar -> jar.reaches(center, range) && filter.test(jar), found);
        BlockOrder.sortWithinManhattan(found, center, jar -> jar.nearestPart(center));
        if (SourceJarSpec.maxReach() == 0) return found;
        List<SourceJarTile> out = new ArrayList<>(found.size());
        Set<Object> storages = Collections.newSetFromMap(new IdentityHashMap<>());
        for (SourceJarTile jar : found) {
            if (storages.add(jar.sharedStorage())) {
                out.add(jar);
            }
        }
        return out;
    }

    private static @Nullable IAdvancedSourceTile sourceTileAt(Level world, BlockPos from, BlockEntity be) {
        BlockPos at = be.getBlockPos();
        if (!world.isLoaded(at) || world.getBlockEntity(at) != be) return null;
        return be.getCapability(CapabilityRegistry.SOURCE_TILE, IAdvancedSourceTile.getDirTo(from, at)).orElse(null);
    }

    public static @Nullable ISpecialSourceProvider takeSource(BlockPos pos, Level level, int range, int source){
        return takeSource(pos, level, range, (long) source);
    }

    public static @Nullable ISpecialSourceProvider takeSource(BlockPos pos, Level level, int range, long source){
        List<ISpecialSourceProvider> providers = canTakeSource(pos, level, range);
        for(ISpecialSourceProvider provider : providers){
            var tile = provider.getSource();
            if(tile.getSourceLong() >= source){
                long taken = tile.removeSource(source, false);
                if(taken >= source){
                    return provider;
                }
                if(taken > 0){
                    tile.addSource(taken, false);
                }
            }
        }
        return null;
    }

    public static @Nullable ISpecialSourceProvider takeSourceWithParticles(BlockPos pos, Level level, int range, int source){
        return takeSourceWithParticles(pos, level, range, (long) source);
    }

    public static @Nullable ISpecialSourceProvider takeSourceWithParticles(BlockPos pos, Level level, int range, long source){
        ISpecialSourceProvider result = takeSource(pos, level, range, source);
        if(result != null){
            EntityFollowProjectile aoeProjectile = new EntityFollowProjectile(level, result.getCurrentPos(), pos);
            ClientFx.spawn(level, aoeProjectile);
        }
        return result;
    }

    /**
     * Searches for nearby mana jars that have enough mana.
     * Returns the position where the source was taken, or null if none were found.
     */
    public static boolean hasSourceNearby(BlockPos pos, Level world, int range, int source) {
        return findSourceNearby(pos, world, range, source);
    }

    private static boolean findSourceNearby(BlockPos pos, Level world, int range, long source) {
        BlockRange box = BlockRange.box(range);
        if (ArsBlockEntityIndexes.SOURCE_JARS.findAny(world, pos, BlockRange.box(range + SourceJarSpec.maxReach()),
                jar -> jar.reaches(pos, range) && jar.getSourceLong() >= source) != null) {
            return true;
        }
        return ArsBlockEntityIndexes.SOURCE_TILES.findAny(world, pos, box, be -> {
            IAdvancedSourceTile tile = sourceTileAt(world, pos, be);
            return tile != null && tile.getSource() >= source && tile.relayCanTakePower();
        }) != null;
    }
}
