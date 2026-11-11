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

    private ModEffects() {
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}
