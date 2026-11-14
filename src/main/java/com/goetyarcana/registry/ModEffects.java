package com.goetyarcana.registry;

import com.goetyarcana.GoetyArcanaMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, GoetyArcanaMod.MOD_ID);

    /** 处决：5 秒后结算。 */
    public static final RegistryObject<MobEffect> EXECUTION =
            EFFECTS.register("execution", com.goetyarcana.effect.ExecutionEffect::new);

    /** 虚空之眼：弩命中叠层。 */
    public static final RegistryObject<MobEffect> VOID_EYE =
            EFFECTS.register("void_eye", com.goetyarcana.effect.VoidEyeEffect::new);

    /** 虚空爆炸：4 秒后爆炸。 */
    public static final RegistryObject<MobEffect> VOID_EXPLOSION =
            EFFECTS.register("void_explosion", com.goetyarcana.effect.VoidExplosionEffect::new);

    /** 流血：每秒 2 点伤害，持续 6 秒。 */
    public static final RegistryObject<MobEffect> BLEED =
            EFFECTS.register("bleed", com.goetyarcana.effect.BleedEffect::new);

    private ModEffects() {
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}


