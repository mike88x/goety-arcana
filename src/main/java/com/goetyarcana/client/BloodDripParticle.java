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
 * 流血粒子：一颗会往下滴的血滴。
 *
 * <p>有重力、会落地，落地后快速淡出。两帧贴图由
 * {@code tools/gen_blood_drip.py} 生成，贴图列表见
 * {@code assets/goetyarcana/particles/blood_drip.json}。</p>
 */
public class BloodDripParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    private static final float START_R = 0.62F;
    private static final float START_G = 0.10F;
    private static final float START_B = 0.19F;

    protected BloodDripParticle(ClientLevel level, double x, double y, double z,
                                double vx, double vy, double vz,
                                SpriteSet sprites, RandomSource random) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.quadSize = 0.075F + random.nextFloat() * 0.035F;
        this.lifetime = 30 + random.nextInt(16);
        this.gravity = 0.16F;
        this.friction = 0.98F;
        this.hasPhysics = true;
        this.alpha = 1.0F;
        this.setColor(START_R, START_G, START_B);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        if (this.removed) {
            return;
        }
        float t = Mth.clamp(this.age / (float) this.lifetime, 0.0F, 1.0F);
        // 落地后快速淡出
        if (this.onGround && this.age > 4) {
            this.alpha = Math.max(0.0F, this.alpha - 0.15F);
        }
        this.alpha *= 1.0F - t * 0.15F;
        this.setColor(START_R * (1.0F - t * 0.35F), START_G, START_B * (1.0F - t * 0.1F));
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        // 血迹在暗处也要看得见
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
            return new BloodDripParticle(level, x, y, z, vx, vy, vz, this.sprites, level.random);
        }
    }
}
