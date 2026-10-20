package com.hollingsworth.arsnouveau.api.util;

import com.hollingsworth.arsnouveau.common.network.ClientFx;
import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.source.IAdvancedSourceTile;
import com.hollingsworth.arsnouveau.api.source.ManhattanOrder;
import com.hollingsworth.arsnouveau.api.source.SourceIndex;
import com.hollingsworth.arsnouveau.api.source.SourceTileIndex;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.hollingsworth.arsnouveau.api.source.SourceProvider;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.common.entity.EntityFollowProjectile;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = ArsNouveau.MODID)
public class SourceUtil {

    public static List<ISpecialSourceProvider> canGiveSource(BlockPos pos, Level world, int range) {
        List<ISpecialSourceProvider> posList = new ArrayList<>();
        for (SourceJarTile jar : SourceIndex.jarsAround(world, pos, range, SourceJarTile::canAcceptSource)) {
            posList.add(new SourceProvider(jar, jar.getBlockPos()));
        }
        List<ISpecialSourceProvider> provider = SourceManager.INSTANCE.canGiveSourceNearby(pos, world, range);
        for(ISpecialSourceProvider p : provider){
            posList.add(new SourceProvider(p));
        }
        List<ISpecialSourceProvider> found = new ArrayList<>();
        SourceTileIndex.forEachAround(world, pos, range, be -> {
            IAdvancedSourceTile tile = sourceTileAt(world, pos, be);
            if (tile != null && tile.canAcceptSource() && tile.sourcelinksCanProvidePower()) {
                found.add(new SourceProvider(tile, be.getBlockPos().immutable()));
            }
        });
        ManhattanOrder.sort(found, pos, ISpecialSourceProvider::getCurrentPos);
        posList.addAll(found);
        return posList;
    }

    public static List<ISpecialSourceProvider> canTakeSource(BlockPos pos, Level world, int range) {
        List<ISpecialSourceProvider> posList = new ArrayList<>();
        for (SourceJarTile jar : SourceIndex.jarsAround(world, pos, range, jar -> jar.getSourceLong() > 0)) {
            posList.add(new SourceProvider(jar, jar.getBlockPos()));
        }
        List<ISpecialSourceProvider> provider = SourceManager.INSTANCE.canTakeSourceNearby(pos, world, range);
        for(ISpecialSourceProvider p : provider){
            posList.add(new SourceProvider(p));
        }
        List<ISpecialSourceProvider> found = new ArrayList<>();
        SourceTileIndex.forEachAround(world, pos, range, be -> {
            IAdvancedSourceTile tile = sourceTileAt(world, pos, be);
            if (tile != null && tile.getSource() > 0 && tile.relayCanTakePower()) {
                found.add(new SourceProvider(tile, be.getBlockPos().immutable()));
            }
        });
        ManhattanOrder.sort(found, pos, ISpecialSourceProvider::getCurrentPos);
        posList.addAll(found);
        return posList;
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
            if(provider.getSource().getSourceLong() >= source){
                provider.getSource().removeSourceLong(source);
                return provider;
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
        if (SourceIndex.anyJarAround(world, pos, range, jar -> jar.getSourceLong() >= source)) {
            return true;
        }
        if (SourceTileIndex.anyAround(world, pos, range, be -> {
            IAdvancedSourceTile tile = sourceTileAt(world, pos, be);
            return tile != null && tile.getSource() >= source && tile.relayCanTakePower();
        })) {
            return true;
        }
        return SourceManager.INSTANCE.hasSourceNearby(pos, world, range, source) != null;
    }
}
