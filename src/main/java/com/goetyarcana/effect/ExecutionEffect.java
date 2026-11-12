package com.goetyarcana.effect;

import com.goetyarcana.VoidScytheBalance;
import com.goetyarcana.combat.CombatTier;
import com.goetyarcana.entity.ExecutionSlashEntity;
import com.goetyarcana.network.ModNetwork;
import com.goetyarcana.registry.ModDamageTypes;
import com.goetyarcana.registry.ModEffects;
import com.goetyarcana.registry.ModParticles;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 处决（Execution）debuff。
 *
 * <p>持续 5 秒；到期时：</p>
 * <ul>
 *   <li>普通敌人：直接消灭（{@link LivingEntity#kill()}，不经过 Boss 限伤）；</li>
 *   <li>精英：造成最大生命值 10% 的伤害；</li>
 *   <li>Boss：造成最大生命值 20% 的伤害。</li>
 * </ul>
 *
 * <p>精英/Boss 的伤害使用自定义伤害类型 {@code goetyarcana:execution}，
 * 它在 {@code minecraft:bypasses_invulnerability} 标签里，因此可以绕过
 * Goety 的 Boss 伤害上限。</p>
 */
public class ExecutionEffect extends MobEffect {

    private static final String OWNER_TAG = "goetyarcana:execution_owner";

    public ExecutionEffect() {
        super(MobEffectCategory.HARMFUL, 0x6A1B9A);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 每 tick 跑一次：前 4.95 秒只做粒子，最后一 tick 结算。
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (level.isClientSide) {
            return;
        }

        MobEffectInstance instance = entity.getEffect(this);
        if (instance == null) {
            return;
        }

        if (instance.getDuration() <= 1) {
            // 不要在 MobEffectInstance.tick() 的迭代过程中直接 kill()/hurt()：
            // 死亡会移除效果，可能让 LivingEntity.tickEffects() 的 keySet 迭代器
            // 抛 ConcurrentModificationException。把结算排到当前 tick 之后。
            scheduleExecution(entity);
            return;
        }

        if (instance.getDuration() % 10 == 0 && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.6D, entity.getZ(),
                    3, 0.35D, 0.45D, 0.35D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                    1, 0.25D, 0.35D, 0.25D, 0.01D);
        }
    }

    /**
     * 由近战/剑气调用：先掷 5% 概率，再挂 5 秒处决。
     *
     * @return 是否成功施加
     */
    public static boolean tryApply(LivingEntity target, @Nullable Player source) {
        if (target.hasEffect(ModEffects.EXECUTION.get())) {
            return false;
        }
        if (target.getRandom().nextFloat() >= VoidScytheBalance.EXECUTION_CHANCE) {
            return false;
        }

        if (source != null) {
            target.getPersistentData().putUUID(OWNER_TAG, source.getUUID());
        } else {
            target.getPersistentData().remove(OWNER_TAG);
        }

        target.addEffect(new MobEffectInstance(
                ModEffects.EXECUTION.get(),
                VoidScytheBalance.EXECUTION_TICKS,
                0,
                false,
                true,
                true));

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    18, 0.4D, 0.5D, 0.4D, 0.03D);
            serverLevel.playSound(null,
                    target.getX(), target.getY(), target.getZ(),
                    SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS,
                    0.8F, 0.55F);
        }
        return true;
    }

    private static void scheduleExecution(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        CompoundTag data = target.getPersistentData();
        UUID ownerId = data.hasUUID(OWNER_TAG) ? data.getUUID(OWNER_TAG) : null;
        data.remove(OWNER_TAG);

        serverLevel.getServer().tell(new TickTask(
                serverLevel.getServer().getTickCount(),
                () -> execute(target, ownerId)));
    }

    private static void execute(LivingEntity target, @Nullable UUID ownerId) {
        if (!(target.level() instanceof ServerLevel serverLevel) || !target.isAlive()) {
            return;
        }

        // 先播放专属斩击动画，再延迟结算；动画实体由客户端渲染成一把巨型虚空镰刀。
        serverLevel.addFreshEntity(new ExecutionSlashEntity(serverLevel, target));
        serverLevel.sendParticles(ModParticles.VOID_FLAME.get(),
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                28, 0.45D, 0.6D, 0.45D, 0.03D);
        serverLevel.playSound(null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.85F, 0.55F);

        serverLevel.getServer().tell(new TickTask(
                serverLevel.getServer().getTickCount() + VoidScytheBalance.EXECUTION_ANIMATION_TICKS,
                () -> resolveExecution(target, ownerId)));
    }

    private static void resolveExecution(LivingEntity target, @Nullable UUID ownerId) {
        if (!(target.level() instanceof ServerLevel serverLevel) || !target.isAlive()) {
            return;
        }

        Player owner = ownerId == null
                ? null
                : serverLevel.getServer().getPlayerList().getPlayer(ownerId);

        CombatTier.Tier tier = CombatTier.of(target);

        // 动画的高潮：再放一轮爆发，然后结算。普通怪调用 kill()，避免任何
        // “伤害上限/减伤”把它救下来。
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                40, 0.5D, 0.7D, 0.5D, 0.04D);
        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                target.getX(), target.getY() + target.getBbHeight() * 0.4D, target.getZ(),
                18, 0.35D, 0.5D, 0.35D, 0.03D);
        serverLevel.sendParticles(ModParticles.VOID_FLAME.get(),
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                48, 0.55D, 0.7D, 0.55D, 0.04D);
        serverLevel.playSound(null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 1.0F, 0.55F);

        switch (tier) {
            case BOSS -> {
                float amount = Math.max(1.0F,
                        target.getMaxHealth() * VoidScytheBalance.EXECUTION_BOSS_PERCENT);
                target.hurt(executionDamage(serverLevel, owner), amount);
            }
            case ELITE -> {
                float amount = Math.max(1.0F,
                        target.getMaxHealth() * VoidScytheBalance.EXECUTION_ELITE_PERCENT);
                target.hurt(executionDamage(serverLevel, owner), amount);
            }
            case NORMAL -> target.kill();
        }

        // 无论普通怪还是被打死的精英/Boss，只要确实死于这次处决，
        // 就通知附近客户端播放专属的虚空湮灭死亡动画（替代原版侧倒）。
        if (!target.isAlive()) {
            ModNetwork.sendExecutionDeath(serverLevel, target);
        }
    }

    private static DamageSource executionDamage(ServerLevel level, @Nullable Player owner) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ModDamageTypes.EXECUTION);
        return new DamageSource(holder, null, owner);
    }
}
