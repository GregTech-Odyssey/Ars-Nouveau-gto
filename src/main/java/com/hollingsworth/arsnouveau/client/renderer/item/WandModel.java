package com.hollingsworth.arsnouveau.client.renderer.item;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.common.items.Wand;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WandModel extends GeoModel<Wand> {

    @Override
    public ResourceLocation getModelResource(Wand wand) {
        return ArsNouveau.id( "geo/wand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Wand wand) {
        return ArsNouveau.id( "textures/item/wand.png");
    }

    @Override
    public ResourceLocation getAnimationResource(Wand wand) {
        return ArsNouveau.id( "animations/wand_animation.json");
    }
}
