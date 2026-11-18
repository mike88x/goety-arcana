package com.goetyarcana.event;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.combat.VoidBlastTags;
import com.goetyarcana.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 砰砰诅咒的生命上限处理。
 *
 * <p>普通怪物：效果存续期间生命上限被压到 1（每 tick 检查一次，效果消失就撤掉修正）；
 * 精英 / BOSS 不吃这个修正，改由 {@code VoidBlastCurseTracker} 记「下次爆炸 +50%」。</p>
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VoidBlastEvents {

    private static final UUID HP_CAP_MODIFIER =
            UUID.fromString("6f1f4f3e-0f1d-4d7a-9a4c-3a2f5f7c1b02");

    private VoidBlastEvents() {
    }

    /** 记账表里还挂着诅咒的 BOSS：效果被清掉就补回来，图标/倒计时始终在。 */
    private static void reapplyCurse(LivingEntity entity, int now) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel)) {
            return;
        }
        if (entity.hasEffect(ModEffects.VOID_BLAST_CURSE.get())) {
            return;
        }
        com.goetyarcana.combat.VoidBlastCurseTracker.mark(entity, now,
                com.goetyarcana.VoidBlastBalance.CURSE_TICKS,
                com.goetyarcana.VoidBlastBalance.CURSE_BLAST_BONUS);
        net.minecraft.world.effect.MobEffectInstance instance =
                new net.minecraft.world.effect.MobEffectInstance(ModEffects.VOID_BLAST_CURSE.get(),
                        com.goetyarcana.VoidBlastBalance.CURSE_TICKS, 0, false, true, true);
        if (!entity.addEffect(instance)) {
            entity.forceAddEffect(instance, null);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        if (VoidBlastTags.isEliteOrBoss(entity)
                && entity.level() instanceof net.minecraft.server.level.ServerLevel server
                && com.goetyarcana.combat.VoidBlastCurseTracker.isCursed(entity, (int) server.getGameTime())) {
            reapplyCurse(entity, (int) server.getGameTime());
        }
        net.minecraft.world.effect.MobEffectInstance curse =
                entity.getEffect(ModEffects.VOID_BLAST_CURSE.get());
        if (curse != null && curse.getAmplifier() >= 1 && curse.getDuration() == 1) {
            // 二级诅咒到期时先咬一口：无视护甲/抗性/附魔/无敌帧
            net.minecraft.world.damagesource.DamageSource source =
                    com.goetyarcana.registry.ModDamageTypes.source(entity.level(),
                            com.goetyarcana.registry.ModDamageTypes.VOID_BLAST, null, null);
            float before = entity.getHealth();
            entity.hurt(source, 1.0F);
            if (before - entity.getHealth() < 1.0F && entity.isAlive()) {
                float remaining = entity.getHealth() - 1.0F;
                if (remaining <= 0.0F) {
                    entity.setHealth(0.0F);
                    entity.die(source);
                } else {
                    entity.setHealth(remaining);
                }
            }
        }
        boolean cursed = entity.hasEffect(ModEffects.VOID_BLAST_CURSE.get())
                && !VoidBlastTags.isEliteOrBoss(entity);
        AttributeModifier existing = maxHealth.getModifier(HP_CAP_MODIFIER);

        if (cursed) {
            if (existing == null) {
                double delta = 1.0D - maxHealth.getValue();
                maxHealth.addTransientModifier(new AttributeModifier(HP_CAP_MODIFIER,
                        "goetyarcana.void_blast_curse", delta, AttributeModifier.Operation.ADDITION));
                if (entity.getHealth() > 1.0F) {
                    entity.setHealth(1.0F);
                }
            }
        } else if (existing != null) {
            maxHealth.removeModifier(HP_CAP_MODIFIER);
            if (entity.getHealth() > entity.getMaxHealth()) {
                entity.setHealth(entity.getMaxHealth());
            }
        }
    }
}
