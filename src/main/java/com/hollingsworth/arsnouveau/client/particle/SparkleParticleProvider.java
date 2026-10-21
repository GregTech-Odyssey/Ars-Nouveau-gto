package com.hollingsworth.arsnouveau.client.particle;

import com.suried.gtorenders.effect.MoteLook;
import com.suried.gtorenders.effect.MoteParticles;
import com.suried.gtorenders.effect.MoteSprite;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;

public class SparkleParticleProvider implements ParticleProvider<ColoredDynamicTypeData> {
    private final MoteLook look;

    public SparkleParticleProvider(SpriteSet sprite) {
        this.look = MoteLook.sprite(MoteSprite.of(sprite));
    }

    @Override
    public Particle createParticle(ColoredDynamicTypeData data, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        MoteParticles.spawn(x, y, z, ParticleUtil.inRange(-0.01, 0.01), -0.02, ParticleUtil.inRange(-0.01, 0.01), 0.98f, 0, data.age, data.scale * 2,
                MoteParticles.CONSTANT, data.color.getColor() & 0xFFFFFF, 1, MoteParticles.FADE, look);
        return null;
    }
}
