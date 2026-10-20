package com.hollingsworth.arsnouveau.common.compat.ae2.client;

import com.hollingsworth.arsnouveau.common.compat.ae2.ArsAE2;
import com.hollingsworth.arsnouveau.common.compat.ae2.PortableSourceCellItem;
import com.hollingsworth.arsnouveau.common.compat.ae2.SourceKey;
import com.hollingsworth.arsnouveau.common.compat.ae2.SourceKeyType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import appeng.api.client.AEKeyRendering;
import appeng.api.client.StorageCellModels;
import appeng.items.storage.BasicStorageCell;

public final class ArsAE2Client {

    private ArsAE2Client() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ArsAE2Client::setup);
        modBus.addListener(ArsAE2Client::registerColors);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(ArsAE2.ME_SOURCE_JAR_ENTITY, MESourceJarBlockEntityRenderer::new));
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            AEKeyRendering.register(SourceKeyType.TYPE, SourceKey.class, SourceRenderer.INSTANCE);
            var driveCell = ArsAE2.id("block/source_drive_cell");
            ArsAE2.getCells().forEach(cell -> StorageCellModels.registerModel(cell, driveCell));
            ArsAE2.getPortables().forEach(cell -> StorageCellModels.registerModel(cell, driveCell));
            StorageCellModels.registerModel(ArsAE2.CREATIVE_SOURCE_CELL, driveCell);
        });
    }

    private static void registerColors(RegisterColorHandlersEvent.Item event) {
        ArsAE2.getCells().forEach(cell -> event.register(BasicStorageCell::getColor, cell));
        ArsAE2.getPortables().forEach(cell -> event.register(PortableSourceCellItem::getColor, cell));
    }
}
