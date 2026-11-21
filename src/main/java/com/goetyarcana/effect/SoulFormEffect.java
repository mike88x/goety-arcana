package com.goetyarcana.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 灵魂形态：灵魂套的濒死状态，10 秒后消散（死亡）。
 */
public class SoulFormEffect extends MobEffect {

    /** 10 秒。 */
    public static final int DURATION = 200;

    public SoulFormEffect() {
        super(MobEffectCategory.HARMFUL, 0x7FD8F0);
    }
}
