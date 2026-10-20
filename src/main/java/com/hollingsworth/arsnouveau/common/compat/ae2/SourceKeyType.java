package com.hollingsworth.arsnouveau.common.compat.ae2;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;

public class SourceKeyType extends AEKeyType {
    public static final ResourceLocation ID = ArsAE2.id("source");
    @Deprecated(since = "4.12.12", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "2027.6.1")
    public static final ResourceLocation LEGACY_ID = new ResourceLocation("arseng", "source");
    public static final Component SOURCE = Component.translatable("ars_nouveau.category.source");
    public static final AEKeyType TYPE = new SourceKeyType();

    private SourceKeyType() {
        super(ID, SourceKey.class, SOURCE);
    }

    @Nullable
    @Override
    public AEKey readFromPacket(FriendlyByteBuf friendlyByteBuf) {
        return SourceKey.KEY;
    }

    @Nullable
    @Override
    public AEKey loadKeyFromTag(CompoundTag compoundTag) {
        return SourceKey.KEY;
    }

    @Override
    public int getAmountPerByte() {
        return 1000;
    }

    @Override
    public int getAmountPerOperation() {
        return 1000;
    }
}
