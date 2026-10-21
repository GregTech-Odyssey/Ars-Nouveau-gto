package com.hollingsworth.arsnouveau.client.particle;

import com.suried.gtorenders.effect.MoteLook;
import com.suried.gtorenders.effect.MoteParticles;
import com.suried.gtorenders.effect.MoteSprite;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;

public class GlowParticleProvider implements ParticleProvider<ColorParticleTypeData> {
    static final float AGING = 6f / 7f;
    private final MoteLook look;

    public GlowParticleProvider(SpriteSet sprite) {
        this.look = MoteLook.spinning(MoteSprite.of(sprite), 1f);
    }

    @Override
    public Particle createParticle(ColorParticleTypeData data, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        int flags = MoteParticles.FADE | (data.disableDepthTest ? MoteParticles.THROUGH_WALLS : 0);
        MoteParticles.spawn(x, y, z, xSpeed * 2, ySpeed * 2, zSpeed * 2, 0.98f, 0, data.age * 0.5f * AGING, data.size * 2, MoteParticles.SHRINK,
                data.color.getColor() & 0xFFFFFF, data.alpha, flags, look);
        return null;
    }
}
