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
 * 自制「虚空火」粒子。
 *
 * <p>不是原版灵魂火：贴图是单独绘制的紫黑色火焰，粒子本身会在生命周期内
 * 从亮白紫向深紫/幽蓝过渡、缓慢上浮、缩小并淡出，还会轻微自旋。
 * 它同时用于：</p>
 * <ul>
 *   <li>手持虚空镰刀时环绕玩家的光带；</li>
 *   <li>右键蓄力时向胸口收束的螺旋；</li>
 *   <li>剑气飞行拖尾与命中/消散爆发。</li>
 * </ul>
 */
public class VoidFlameParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float baseSize;

    // 颜色从亮紫白过渡到深紫蓝。
    private static final float START_R = 0.82F;
    private static final float START_G = 0.62F;
    private static final float START_B = 1.00F;
    private static final float END_R = 0.32F;
    private static final float END_G = 0.10F;
    private static final float END_B = 0.78F;

    protected VoidFlameParticle(ClientLevel level, double x, double y, double z,
                                double vx, double vy, double vz,
                                SpriteSet sprites, RandomSource random) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.baseSize = 0.075F + random.nextFloat() * 0.075F;
        this.quadSize = this.baseSize;
        this.lifetime = 18 + random.nextInt(14);
        this.gravity = 0.0F;
        this.friction = 0.94F;
        this.hasPhysics = false;
        this.alpha = 0.0F;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.setSpriteFromAge(sprites);
        this.setColor(START_R, START_G, START_B);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();

        if (this.removed) {
            return;
        }

        // 缓慢上浮 + 轻微横向涡流，让火焰看起来在“燃烧”。
        this.yd += 0.0018D;
        float swirl = (float) Math.sin((this.age + this.roll) * 0.22F) * 0.0007F;
        this.xd += swirl;
        this.zd -= swirl;

        float t = Mth.clamp(this.age / (float) this.lifetime, 0.0F, 1.0F);

        // 出现时快速淡入，最后 45% 淡出。
        float fadeIn = Mth.clamp(t / 0.12F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((1.0F - t) / 0.45F, 0.0F, 1.0F);
        this.alpha = 0.85F * fadeIn * fadeOut;

        // 前 25% 稍微膨胀，之后缓慢收缩。
        if (t < 0.25F) {
            this.quadSize = this.baseSize * (0.75F + t * 1.4F);
        } else {
            this.quadSize *= 0.982F;
        }

        this.roll += 0.035F;
        this.setColor(
                Mth.lerp(t, START_R, END_R),
                Mth.lerp(t, START_G, END_G),
                Mth.lerp(t, START_B, END_B));
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        // 虚空火自带发光，不吃方块光照，洞穴/夜晚也能保持紫白色。
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
            return new VoidFlameParticle(level, x, y, z, vx, vy, vz, this.sprites, level.random);
        }
    }
}
