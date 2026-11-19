package com.goetyarcana.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 砰砰诅咒：没被虚空苦力怕炸死时留下的后遗症。
 *
 * <p>受到的伤害 +40%，若伤害类型是爆炸则 +80%；具体结算在
 * {@code event/BombCurseEvents}。</p>
 */
public class BombCurseEffect extends MobEffect {

    /** 最短 30 秒。 */
    public static final int MIN_TICKS = 20 * 30;
    /** 最长 3 分钟。 */
    public static final int MAX_TICKS = 20 * 60 * 3;

    public BombCurseEffect() {
        super(MobEffectCategory.HARMFUL, 0xB03050);
    }
}
