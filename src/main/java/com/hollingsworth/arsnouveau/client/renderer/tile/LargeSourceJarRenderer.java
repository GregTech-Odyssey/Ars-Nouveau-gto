package com.hollingsworth.arsnouveau.client.renderer.tile;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.common.block.LargeSourceJar;
import com.hollingsworth.arsnouveau.common.block.tile.LargeSourceJarTile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

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
        int y0 = bottom - 16;
        int y1 = segment - 16;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(FLUID);
        VertexConsumer buffer = buffers.getBuffer(RenderType.translucent());
        Matrix4f pose = poseStack.last().pose();
        for (int a = MIN; a < MAX; a += 16) {
            int a1 = Math.min(MAX, a + 16);
            for (int y = y0; y < y1; y += 16) {
                int yb = Math.min(y1, y + 16);
                float u0 = sprite.getU(0), u1 = sprite.getU(a1 - a), v0 = sprite.getV(16 - (yb - y)), v1 = sprite.getV(16);
                quad(buffer, pose, light, u0, v0, u1, v1, 0, 0, -1, a, y, MIN, a, yb, MIN, a1, yb, MIN, a1, y, MIN);
                quad(buffer, pose, light, u0, v0, u1, v1, 0, 0, 1, a1, y, MAX, a1, yb, MAX, a, yb, MAX, a, y, MAX);
                quad(buffer, pose, light, u0, v0, u1, v1, -1, 0, 0, MIN, y, a1, MIN, yb, a1, MIN, yb, a, MIN, y, a);
                quad(buffer, pose, light, u0, v0, u1, v1, 1, 0, 0, MAX, y, a, MAX, yb, a, MAX, yb, a1, MAX, y, a1);
            }
            if (segment < LargeSourceJarTile.SEGMENT_PIXELS) {
                for (int b = MIN; b < MAX; b += 16) {
                    int b1 = Math.min(MAX, b + 16);
                    float u0 = sprite.getU(0), u1 = sprite.getU(a1 - a), v0 = sprite.getV(0), v1 = sprite.getV(b1 - b);
                    quad(buffer, pose, light, u0, v0, u1, v1, 0, 1, 0, a, y1, b, a, y1, b1, a1, y1, b1, a1, y1, b);
                }
            }
        }
    }

    private static void quad(VertexConsumer buffer, Matrix4f pose, int light, float u0, float v0, float u1, float v1, float nx, float ny, float nz,
                             float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3) {
        vertex(buffer, pose, x0, y0, z0, u0, v1, light, nx, ny, nz);
        vertex(buffer, pose, x1, y1, z1, u0, v0, light, nx, ny, nz);
        vertex(buffer, pose, x2, y2, z2, u1, v0, light, nx, ny, nz);
        vertex(buffer, pose, x3, y3, z3, u1, v1, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f pose, float x, float y, float z, float u, float v, int light, float nx, float ny, float nz) {
        buffer.vertex(pose, x / 16F, y / 16F, z / 16F).color(255, 255, 255, 255).uv(u, v).uv2(light).normal(nx, ny, nz).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(LargeSourceJarTile tile) {
        return true;
    }
}
