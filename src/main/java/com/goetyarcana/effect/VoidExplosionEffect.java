package com.goetyarcana.effect;

import com.goetyarcana.combat.VoidExplosion;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 虚空爆炸：4 秒后以携带者为中心爆炸。
 */
public class VoidExplosionEffect extends MobEffect {

    public VoidExplosionEffect() {
        super(MobEffectCategory.HARMFUL, 0x9B5CF6);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration <= 1;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            VoidExplosion.explode(entity.level(), entity.position(), entity.getLastHurtByMob());
        }
    }
}

