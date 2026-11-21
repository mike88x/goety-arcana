package com.goetyarcana.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 虚空印记：每层减少目标 10% 防御、15% 伤害，最多 5 层，满层时爆炸。
 */
public class VoidMarkEffect extends MobEffect {

    public static final int MAX_STACKS = 5;

    public VoidMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B5CF6);
    }
}
