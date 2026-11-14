package com.goetyarcana.combat;

import com.goetyarcana.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 统一的虚空爆炸：紫焰球 + 两圈扩散环 + 反向传送门火花 + 魔法伤害。
 *
 * <p>表现层整体抬 0.3 格：贴脸爆炸时双环不会被摄像机包在正中间变成一圈大方块，
 * 紫焰粒子也收得更紧（28 颗 / spread 0.8），避免在 1~2 格外糊成大白团。</p>
 */
public final class VoidExplosion {

    /** 表现层相对爆炸点抬高多少格。 */
    private static final double VISUAL_LIFT = 0.3D;

    private VoidExplosion() {
    }

    public static void explode(Level level, Vec3 center, Entity... excludes) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        double x = center.x;
        double y = center.y + VISUAL_LIFT;
        double z = center.z;
        server.addFreshEntity(new com.goetyarcana.entity.VoidExplosionEntity(server, new Vec3(x, y, z)));
        server.sendParticles(ModParticles.VOID_FLAME.get(), x, y, z, 28,
                0.8D, 0.8D, 0.8D, 0.05D);
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 8,
                0.7D, 0.7D, 0.7D, 0.02D);
        for (int ring = 0; ring < 2; ring++) {
            double radius = ring == 0 ? 1.2D : 2.2D;
            for (int i = 0; i < 16; i++) {
                double angle = i * Math.PI * 2.0D / 16.0D;
                server.sendParticles(ModParticles.VOID_FLAME.get(),
                        x + Math.cos(angle) * radius, y + 0.1D, z + Math.sin(angle) * radius,
                        1, 0.0D, 0.05D, 0.0D, 0.0D);
            }
        }
        server.playSound(null, center.x, center.y + VISUAL_LIFT, center.z, SoundEvents.ENDER_DRAGON_HURT,
                SoundSource.PLAYERS, 1.0F, 0.6F);
        server.playSound(null, center.x, center.y + VISUAL_LIFT, center.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 0.9F, 1.1F);

        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center, center).inflate(3.2D), e -> e.isAlive())) {
            boolean skip = false;
            for (Entity exclude : excludes) {
                if (target == exclude) {
                    skip = true;
                    break;
                }
            }
            if (skip) {
                continue;
            }
            double distance = Math.sqrt(target.distanceToSqr(center));
            float factor = (float) Math.max(0.3D, 1.0D - distance / 5.0D);
            target.hurt(target.damageSources().magic(), 10.0F * factor);
            Vec3 push = target.position().subtract(center).normalize().scale(0.5D);
            target.push(push.x, 0.25D, push.z);
        }
    }
}


