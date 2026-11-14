package com.goetyarcana.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 虚空之眼：虚空弩命中后获得的层数效果。
 *
 * <p>实际装填/箭速逻辑在 {@code VoidCrossbowItem} 与
 * {@code VoidCrossbowEvents} 中读取层数，不靠属性修改器。</p>
 */
public class VoidEyeEffect extends MobEffect {

    public VoidEyeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x67E8F9);
    }
}
