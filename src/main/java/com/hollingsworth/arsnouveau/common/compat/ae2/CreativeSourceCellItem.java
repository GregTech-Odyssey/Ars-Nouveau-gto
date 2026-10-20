package com.hollingsworth.arsnouveau.common.compat.ae2;

import net.minecraft.world.item.Rarity;

import appeng.items.AEBaseItem;

public class CreativeSourceCellItem extends AEBaseItem {
    public CreativeSourceCellItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }
}
