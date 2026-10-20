package com.hollingsworth.arsnouveau.common.compat.ae2;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;

public class CreativeSourceCellHandler implements ICellHandler {
    public static final CreativeSourceCellHandler INSTANCE = new CreativeSourceCellHandler();

    private CreativeSourceCellHandler() {}

    @Override
    public boolean isCell(ItemStack is) {
        return is != null && is.getItem() instanceof CreativeSourceCellItem;
    }

    @Override
    public @Nullable StorageCell getCellInventory(ItemStack is, @Nullable ISaveProvider host) {
        return isCell(is) ? new CreativeSourceCellInventory(is) : null;
    }
}
