package com.goetyarcana.combat;

import com.goetyarcana.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 统一的「虚空爆炸」表现：冲击波环 + 小号爆炸 + 虚空火 + 反向传送门。
 *
 * <p>仆从与自然生成的虚空苦力怕都用这一套，避免一个像原版爆炸、一个像虚空。</p>
 */
public final class VoidBlastVisuals {

    private VoidBlastVisuals() {
    }

    public static void explode(ServerLevel server, double x, double y, double z) {
        server.sendParticles(ParticleTypes.SONIC_BOOM, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        server.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 4, 0.9D, 0.4D, 0.9D, 0.0D);
        server.sendParticles(ModParticles.VOID_FLAME.get(), x, y, z, 45, 1.1D, 0.8D, 1.1D, 0.06D);
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 60, 1.2D, 0.9D, 1.2D, 0.14D);
        server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 18, 0.8D, 0.5D, 0.8D, 0.05D);
        server.playSound(null, x, y, z, com.goetyarcana.registry.ModSounds.VOID_EXPLOSION.get(),
                SoundSource.HOSTILE, 1.6F, 1.0F);
        server.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.7F, 0.5F);
    }
}
