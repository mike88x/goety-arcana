package com.goetyarcana.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 砰砰诅咒（测试名）：被虚空苦力怕仆从炸过、但活下来的目标会中的诅咒。
 *
 * <ul>
 *   <li>普通怪物：生命上限被强制压到 1（血量上限的修正由
 *       {@code event/VoidBlastEvents} 在效果存续期间维护）；</li>
 *   <li>精英 / BOSS：不吃这个 effect，改为记进
 *       {@code combat/VoidBlastCurseTracker}，让下一次爆炸伤害 +50%。</li>
 * </ul>
 */
public class VoidBlastCurseEffect extends MobEffect {

    public VoidBlastCurseEffect() {
        super(MobEffectCategory.HARMFUL, 0x6A3FA8);
    }

    @Override
    public boolean isBeneficial() {
        // 有些 BOSS 的 canBeAffected 只放行「有益」效果，这里放行；
        // 真正的强度差别由 events 里按 elite/boss 标签决定。
        return true;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 生命上限修正由 VoidBlastEvents 维护，这里只做标记与计时。
    }
}
