package com.goetyarcana.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 扎根：禁止瞬移（末影人、末影类 BOSS 的传送都会被拦下）。
 *
 * <p>真正的拦截在 {@code event/BrambleBindEvents#onTeleport}：
 * 取消 Forge 的 {@code EntityTeleportEvent}，只挡「生物自己传送」，
 * 指令 / 传送门之类的照常。</p>
 */
public class RootedEffect extends MobEffect {

    public RootedEffect() {
        super(MobEffectCategory.HARMFUL, 0x6E8C3A);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 只做标记
    }
}
