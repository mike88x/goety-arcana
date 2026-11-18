package com.goetyarcana.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * 荒野符文粒子：仿原版附魔符文笔画的三种荒野字形轮换。
 *
 * <p>贴图 {@code textures/particle/wild_rune_0..2.png}，用于荆棘藤缚的范围指示与种子拖尾。</p>
 */
public class WildRuneParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected WildRuneParticle(ClientLevel level, double x, double y, double z,
                               double vx, double vy, double vz,
                               SpriteSet sprites, RandomSource random) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.quadSize = 0.11F + random.nextFloat() * 0.05F;
        this.lifetime = 16 + random.nextInt(10);
        this.gravity = -0.002F;
        this.friction = 0.96F;
        this.hasPhysics = false;
        this.alpha = 0.0F;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.setSprite(sprites.get(random.nextInt(3), 3));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float t = Mth.clamp(this.age / (float) this.lifetime, 0.0F, 1.0F);
        this.alpha = 0.9F * Mth.clamp(t / 0.15F, 0.0F, 1.0F) * Mth.clamp((1.0F - t) / 0.5F, 0.0F, 1.0F);
        this.quadSize *= 0.988F;
        this.roll += 0.012F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    /** 供 {@code RegisterParticleProvidersEvent#registerSpriteSet} 使用。 */
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new WildRuneParticle(level, x, y, z, vx, vy, vz, this.sprites, level.random);
        }
    }
}
