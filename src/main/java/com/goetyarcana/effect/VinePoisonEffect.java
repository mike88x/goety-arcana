package com.goetyarcana.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 藤毒：穿荒野长袍时，被荆棘藤缚缠住的敌人会中的毒。
 *
 * <p>这个 effect 本身只做标记与计时（图标/剩余时间），真正的伤害由
 * {@code BrambleVinesEntity} 每秒递增结算，伤害类型是 {@code goetyarcana:vine_poison}
 * （绕过护甲、抗性、附魔与无敌帧），所以亡灵生物、免疫负面效果的 BOSS 一样会掉血。</p>
 */
public class VinePoisonEffect extends MobEffect {

    public VinePoisonEffect() {
        super(MobEffectCategory.HARMFUL, 0x4C6B1E);
    }

    /**
     * 伪装成「有益效果」。
     *
     * <p>像 Goety 的末影守望者这类 BOSS 的 {@code canBeAffected} 只放行
     * {@code isBeneficial()} 为真的效果（已用字节码确认：{@code effect.isBeneficial() && super.canBeAffected(...)}），
     * 而 {@code forceAddEffect} 内部同样会调 {@code canBeAffected}，所以普通手段挂不上。
     * 这里只覆写判定，分类仍是 {@link MobEffectCategory#HARMFUL}——HUD 边框、清除逻辑照旧按负面处理。</p>
     */
    @Override
    public boolean isBeneficial() {
        return true;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 伤害在藤蔓实体里结算
    }
}
