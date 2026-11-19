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

    /** 砰砰诅咒：被虚空苦力怕仆从炸后，普通怪生命上限压到 1 / 精英记加成。 */
    public static final RegistryObject<MobEffect> VOID_BLAST_CURSE =
            EFFECTS.register("void_blast_curse", com.goetyarcana.effect.VoidBlastCurseEffect::new);

    /** 藤毒：荒野长袍加成下的持续递增伤害（标记型 effect）。 */
    public static final RegistryObject<MobEffect> VINE_POISON =
            EFFECTS.register("vine_poison", com.goetyarcana.effect.VinePoisonEffect::new);

    /** 砰砰诅咒：没被虚空苦力怕炸死的玩家受到的易伤。 */
    public static final RegistryObject<MobEffect> BOMB_CURSE =
            EFFECTS.register("bomb_curse", com.goetyarcana.effect.BombCurseEffect::new);

    /** 虚空恩庇：护身符救命后被炸出来的减伤，随时间衰减。 */
    public static final RegistryObject<MobEffect> VOID_AEGIS =
            EFFECTS.register("void_aegis", com.goetyarcana.effect.VoidAegisEffect::new);

    /** 扎根：禁止瞬移（替代 Goety 的 ender_ground）。 */
    public static final RegistryObject<MobEffect> ROOTED =
            EFFECTS.register("rooted", com.goetyarcana.effect.RootedEffect::new);

    /** 缠绕：禁止移动与使用物品/技能。 */
    public static final RegistryObject<MobEffect> ENTANGLED =
            EFFECTS.register("entangled", com.goetyarcana.effect.EntangledEffect::new);

    private ModEffects() {
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}


