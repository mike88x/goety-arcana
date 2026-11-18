package com.goetyarcana.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 虚空恩庇：护身符救你一命之后附着在身上的庇护。
 *
 * <p>减伤比例 = 剩余时间 / 总时长（100% → 0%），逻辑在
 * {@code event/VoidAmuletEvents#onLivingHurt}。</p>
 */
public class VoidAegisEffect extends MobEffect {

    /** 效果总时长：10 分钟。 */
    public static final int DURATION = 20 * 60 * 10;

    public VoidAegisEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B5CF6);
    }
}
