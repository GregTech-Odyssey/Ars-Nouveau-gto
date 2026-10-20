package com.hollingsworth.arsnouveau.common.compat.ae2;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseEntityBlock;

public class SourceAcceptorBlock extends AEBaseEntityBlock<SourceAcceptorBlockEntity> {
    public SourceAcceptorBlock() {
        super(AEBaseBlock.defaultProps(MapColor.METAL, SoundType.METAL));
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return ArsAE2.selfDrop(this, builder);
    }
}
