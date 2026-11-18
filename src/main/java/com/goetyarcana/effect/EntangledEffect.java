package com.goetyarcana.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 缠绕：被荒野藤蔓缠住的标记。
 *
 * <p>本身不造成伤害，也不做任何逻辑——「不能移动 / 不能用物品和技能」
 * 全在 {@code event/BrambleBindEvents} 里按这个 effect 拦截。
 * 持续时间由藤蔓实体每 tick 刷新，所以藤蔓一消失就会自然结束。</p>
 */
public class EntangledEffect extends MobEffect {

    public EntangledEffect() {
        super(MobEffectCategory.HARMFUL, 0x4A7D3A);
    }
}
