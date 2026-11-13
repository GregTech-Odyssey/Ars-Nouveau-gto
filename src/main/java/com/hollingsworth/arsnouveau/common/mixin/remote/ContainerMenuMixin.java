package com.hollingsworth.arsnouveau.common.mixin.remote;

import com.hollingsworth.arsnouveau.common.util.RemoteMenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractContainerMenu.class)
public class ContainerMenuMixin implements RemoteMenuAccess.Holder {

    @Unique
    private RemoteMenuAccess.Origin ars_nouveau$remoteOrigin;

    @Override
    public @Nullable RemoteMenuAccess.Origin ars_nouveau$remoteOrigin() {
        return ars_nouveau$remoteOrigin;
    }

    @Override
    public void ars_nouveau$setRemoteOrigin(@Nullable RemoteMenuAccess.Origin origin) {
        ars_nouveau$remoteOrigin = origin;
    }
}
