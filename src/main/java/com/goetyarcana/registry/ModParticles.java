package com.goetyarcana.registry;

import com.goetyarcana.GoetyArcanaMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 本模组的自定义粒子。
 *
 * <p>目前只有一种：{@code void_flame} —— 虚空火。它是自制贴图 + 自制
 * {@code VoidFlameParticle}，用于代替原版灵魂火来做虚空镰刀的环绕光效、
 * 蓄力收束与剑气拖尾。</p>
 */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, GoetyArcanaMod.MOD_ID);

    public static final RegistryObject<SimpleParticleType> VOID_FLAME =
            PARTICLE_TYPES.register("void_flame", () -> new SimpleParticleType(false));

    private ModParticles() {
    }

    public static void register(IEventBus modBus) {
        PARTICLE_TYPES.register(modBus);
    }
}
