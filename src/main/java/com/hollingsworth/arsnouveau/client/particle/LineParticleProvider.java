package com.hollingsworth.arsnouveau.client.particle;

import com.suried.gtorenders.effect.MoteLook;
import com.suried.gtorenders.effect.MoteParticles;
import com.suried.gtorenders.effect.MoteSprite;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LineParticleProvider implements ParticleProvider<ColoredDynamicTypeData> {
    private final MoteLook look;

    public LineParticleProvider(SpriteSet sprite) {
        this.look = MoteLook.sprite(MoteSprite.of(sprite));
    }

    @Override
    public Particle createParticle(ColoredDynamicTypeData data, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        MoteParticles.slide(x, y, z, xSpeed, ySpeed, zSpeed, data.age * GlowParticleProvider.AGING, data.scale * 2, MoteParticles.SHRINK,
                data.color.getColor() & 0xFFFFFF, 1, MoteParticles.FADE, look);
        return null;
    }
}
