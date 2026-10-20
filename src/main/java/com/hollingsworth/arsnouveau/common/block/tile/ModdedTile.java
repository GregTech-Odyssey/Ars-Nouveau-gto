package com.hollingsworth.arsnouveau.common.block.tile;

import com.gto.datasynclib.FieldDataManager;
import com.hollingsworth.arsnouveau.common.network.BlockEntitySync;
import com.hollingsworth.arsnouveau.common.util.RegistryWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class ModdedTile extends BlockEntity implements BlockEntitySync.Holder {
    private BlockEntitySync sync;

    public ModdedTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }

    public ModdedTile(RegistryWrapper<? extends BlockEntityType<?>> tileEntityTypeIn, BlockPos pos, BlockState state) {
        this(tileEntityTypeIn.get(), pos, state);
    }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        BlockEntitySync.request(this);
        return null;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        handleUpdateTag(pkt.getTag() == null ? new CompoundTag() : pkt.getTag());
    }

    public boolean updateBlock() {
        if (level != null) {
            if (level.isClientSide) {
                BlockState state = level.getBlockState(worldPosition);
                level.sendBlockUpdated(worldPosition, state, state, 3);
            } else {
                BlockEntitySync.request(this);
            }
            setChanged();
            return true;
        }
        return false;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return ars$sync().writeFull();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (BlockEntitySync.isFullSync(tag)) {
            ars$sync().readFull(tag);
        } else {
            super.handleUpdateTag(tag);
        }
    }

    protected CompoundTag writeSyncTag() {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        return tag;
    }

    protected void readSyncTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public BlockEntitySync ars$sync() {
        var s = sync;
        if (s == null) {
            sync = s = new BlockEntitySync(this, this::writeSyncTag, this::readSyncTag);
        }
        return s;
    }

    @Override
    public FieldDataManager getFieldDataManager() {
        return ars$sync().manager().get();
    }

    @Override
    public void writeCustomSyncData(FriendlyByteBuf buf, boolean writeAll) {
        ars$sync().writeCustom(buf, writeAll);
    }

    @Override
    public void readCustomSyncData(FriendlyByteBuf buf) {
        ars$sync().readCustom(buf);
    }

    public double getX() {
        return this.worldPosition.getX();
    }

    public double getY() {
        return this.worldPosition.getY();
    }

    public double getZ() {
        return this.worldPosition.getZ();
    }
}
