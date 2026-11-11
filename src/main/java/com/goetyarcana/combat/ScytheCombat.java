package com.goetyarcana.combat;

import com.Polarice3.Goety.common.effects.GoetyEffects;
import com.goetyarcana.VoidScytheBalance;
import com.goetyarcana.effect.ExecutionEffect;
import com.goetyarcana.item.VoidScytheItem;
import com.goetyarcana.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.List;

/**
 * 虚空镰刀的 360° 近战结算。
 *
 * <p>不依赖原版单体攻击：左键事件会取消原版攻击，然后在这里对以玩家为圆心、
 * 半径 {@link VoidScytheBalance#MELEE_RADIUS} 的所有合法敌人各结算一次。
 * 因此背后、侧面和空中的敌人都能被打到。</p>
 */
public final class ScytheCombat {

    /**
     * 尝试打一次 360° 横扫。
     *
     * @return 是否真的挥出了这一刀（冷却未好时返回 false）
     */
    public static boolean trySweep(Player player) {
        if (!(player.level() instanceof ServerLevel level) || player.isSpectator()) {
            return false;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof VoidScytheItem)) {
            return false;
        }

        // 0.9 攻速：与原版攻击冷却一致，蓄满 90% 才允许出手。
        if (player.getAttackStrengthScale(0.5F) < 0.9F) {
            return false;
        }
        player.resetAttackStrengthTicker();

        float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        DamageSource source = player.damageSources().playerAttack(player);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(
                        VoidScytheBalance.MELEE_RADIUS,
                        VoidScytheBalance.MELEE_HEIGHT,
                        VoidScytheBalance.MELEE_RADIUS),
                target -> CombatUtil.isValidTarget(player, target));

        int hitCount = 0;
        for (LivingEntity target : targets) {
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            if (dx * dx + dz * dz > VoidScytheBalance.MELEE_RADIUS * VoidScytheBalance.MELEE_RADIUS) {
                continue;
            }
            if (Math.abs(target.getY() - player.getY()) > VoidScytheBalance.MELEE_HEIGHT) {
                continue;
            }

            float damage = baseDamage
                    + EnchantmentHelper.getDamageBonus(stack, target.getMobType());
            if (target.hurt(source, damage)) {
                hitCount++;
                applyOnHit(target, player);
                target.knockback(0.35D, player.getX() - target.getX(), player.getZ() - target.getZ());
                target.hurtMarked = true;
            }
        }

        if (hitCount > 0) {
            stack.hurtAndBreak(VoidScytheBalance.MELEE_DURABILITY_COST, player,
                    broken -> broken.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }

        sweepEffect(level, player, stack);
        return true;
    }

    /**
     * 左键命中附加：70% 虚空之蚀 I / 30% 虚空之蚀 II，另有 5% 处决。
     */
    public static void applyOnHit(LivingEntity target, Player source) {
        int amplifier = source.getRandom().nextFloat() < VoidScytheBalance.VOID_EROSION_I_CHANCE
                ? 0
                : 1;
        target.addEffect(new MobEffectInstance(
                GoetyEffects.VOID_TOUCHED.get(),
                VoidScytheBalance.VOID_EROSION_TICKS,
                amplifier,
                false,
                true,
                true));

        ExecutionEffect.tryApply(target, source);
    }

    /**
     * 攻击动画的粒子部分：360° 的横扫环 + 内圈虚空粒子 + 挥砍音效。
     */
    private static void sweepEffect(ServerLevel level, Player player, ItemStack stack) {
        double y = player.getY() + player.getBbHeight() * 0.55D;

        // 统一使用自制虚空火：外圈 + 内圈两层 360° 火环，替代原版 SWEEP_ATTACK。
        for (int i = 0; i < 28; i++) {
            double angle = Math.PI * 2.0D * i / 28.0D;
            double x = player.getX() + Math.cos(angle) * 4.1D;
            double z = player.getZ() + Math.sin(angle) * 4.1D;
            level.sendParticles(ModParticles.VOID_FLAME.get(), x, y, z,
                    1, 0.0D, 0.045D, 0.0D, 0.008D);
        }

        for (int i = 0; i < 32; i++) {
            double angle = Math.PI * 2.0D * i / 32.0D;
            double radius = 1.8D + (i % 5) * 0.42D;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            level.sendParticles(ModParticles.VOID_FLAME.get(), x, y + 0.12D, z,
                    1, 0.0D, 0.06D, 0.0D, 0.012D);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.75F);
        // 用护甲附魔/武器本身的存在感给一点低频虚空呼啸。
        if (stack.hasFoil()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.35F, 0.6F);
        }
    }

    private ScytheCombat() {
    }
}
