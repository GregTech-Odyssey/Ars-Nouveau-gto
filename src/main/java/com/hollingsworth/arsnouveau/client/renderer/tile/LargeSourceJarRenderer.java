package com.hollingsworth.arsnouveau.client.renderer.tile;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.common.block.LargeSourceJar;
import com.hollingsworth.arsnouveau.common.block.tile.LargeSourceJarTile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.suried.gtorenders.geometry.SpriteQuads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public class LargeSourceJarRenderer implements BlockEntityRenderer<LargeSourceJarTile> {
    private static final ResourceLocation FLUID = new ResourceLocation(ArsNouveau.MODID, "block/mana_still");
    private static final int MIN = -6;
    private static final int MAX = 22;

    public LargeSourceJarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LargeSourceJarTile tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (!tile.isCore()) {
            return;
        }
        int segment = tile.getShownSegment();
        int bottom = tile.getShownIndex() == 0 ? LargeSourceJar.BASE_PIXELS : 0;
        if (segment <= bottom) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(FLUID);
        int faces = segment < LargeSourceJarTile.SEGMENT_PIXELS ? SpriteQuads.SIDES | SpriteQuads.UP : SpriteQuads.SIDES;
        SpriteQuads.box(buffers.getBuffer(RenderType.translucent()), poseStack.last().pose(), sprite, MIN, bottom - 16, MIN, MAX, segment - 16, MAX,
                faces, 0xFFFFFFFF, light);
    }

    @Override
    public boolean shouldRenderOffScreen(LargeSourceJarTile tile) {
        return true;
    }
}
