package com.hollingsworth.arsnouveau.client.renderer.tile;

import com.hollingsworth.arsnouveau.ArsNouveau;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;


public class GenericModel<T extends GeoAnimatable> extends GeoModel<T> {
    public String path;

    public ResourceLocation modelLocation;
    public ResourceLocation textLoc;
    public ResourceLocation animationLoc;
    public String textPathRoot = "block";
    public String name;

    public GenericModel(String name) {
        this.modelLocation = ArsNouveau.id( "geo/" + name + ".geo.json");
        this.textLoc = ArsNouveau.id( "textures/" + textPathRoot + "/" + name + ".png");
        this.animationLoc = ArsNouveau.id( "animations/" + name + "_animations.json");
        this.name = name;
    }

    public GenericModel(String name, String textPath) {
        this(name);
        this.textPathRoot = textPath;
        this.textLoc = ArsNouveau.id( "textures/" + textPathRoot + "/" + name + ".png");
    }

    public GenericModel withEmptyAnim(){
        this.animationLoc = ArsNouveau.id( "animations/empty.json");
        return this;
    }

    @Override
    public ResourceLocation getModelResource(T GeoAnimatable) {
        return modelLocation;
    }

    @Override
    public ResourceLocation getTextureResource(T GeoAnimatable) {
        return textLoc;
    }

    @Override
    public ResourceLocation getAnimationResource(T GeoAnimatable) {
        return animationLoc;
    }
}