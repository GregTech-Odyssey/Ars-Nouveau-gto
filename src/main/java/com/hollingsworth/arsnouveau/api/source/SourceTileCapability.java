package com.hollingsworth.arsnouveau.api.source;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.common.block.tile.ImbuementTile;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SourceTileCapability {

    private static final ResourceLocation ID = new ResourceLocation(ArsNouveau.MODID, "source_tile");

    private SourceTileCapability() {}

    public static void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof ISourceTile sourceTile)) return;
        var provider = new Provider(sourceTile);
        event.addCapability(ID, provider);
        event.addListener(provider.holder::invalidate);
    }

    private static final class Provider implements ICapabilityProvider {
        private final LazyOptional<IAdvancedSourceTile> holder;

        private Provider(ISourceTile tile) {
            holder = LazyOptional.of(() -> new SourceTileWrapper(tile,
                    tile instanceof AbstractSourceMachine && !(tile instanceof ImbuementTile),
                    tile instanceof SourceJarTile));
        }

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return CapabilityRegistry.SOURCE_TILE.orEmpty(cap, holder);
        }
    }
}
