package com.goetyarcana.entity;

import com.goetyarcana.BrambleBindBalance;
import com.goetyarcana.registry.ModEffects;
import com.goetyarcana.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 缠在目标身上的藤蔓。
 *
 * <p>完全无敌（{@link #isInvulnerable()}、{@link #hurt} 都拦掉），没有碰撞体积。
 * 生成时记下目标的位置/朝向与原来的 {@code noAi} 状态，之后每 tick 把目标钉在原地；
 * {@link BrambleBindBalance#ENTANGLE_TICKS} tick 后自己消失，并把目标状态还原
 * （{@code noAi} 恢复原值、清掉缠绕 effect）。这样不管目标是不是会无视 {@code noAi}
 * 的 BOSS，位置都会被我们按回去。</p>
 */
public class BrambleVinesEntity extends Entity {

    /** 目标的体型，同步给客户端决定藤蔓多大。 */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_WIDTH =
            net.minecraft.network.syncher.SynchedEntityData.defineId(BrambleVinesEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_HEIGHT =
            net.minecraft.network.syncher.SynchedEntityData.defineId(BrambleVinesEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    /** 这一株的缠绕时长，客户端要靠它排死亡动画。 */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_TICKS =
            net.minecraft.network.syncher.SynchedEntityData.defineId(BrambleVinesEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);

    /** 死亡动画长度（tick）：最后这点时间里藤蔓缩回地下并散成粒子。 */
    public static final int DEATH_TICKS = 14;

    private int life;
    private int targetId = -1;
    /** 缠绕总时长（魔杖 15 秒 / 默认 5 秒）。 */
    private int entangleTicks = BrambleBindBalance.ENTANGLE_TICKS;
    /** 是否附带藤毒（荒野长袍）。 */
    private boolean poison;

    private double frozenX;
    private double frozenY;
    private double frozenZ;
    private float frozenYRot;
    private float frozenXRot;
    private boolean hasFrozen;
    /** 目标原来的 noAi 值；null 表示不是 Mob / 没改过。 */
    private Boolean previousNoAi;

    public BrambleVinesEntity(EntityType<? extends BrambleVinesEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public BrambleVinesEntity(Level level, LivingEntity target) {
        this(level, target, BrambleBindBalance.ENTANGLE_TICKS, false);
    }

    public BrambleVinesEntity(Level level, LivingEntity target, int entangleTicks, boolean poison) {
        this(ModEntities.BRAMBLE_VINES.get(), level);
        this.targetId = target.getId();
        this.entangleTicks = Math.max(20, entangleTicks);
        this.poison = poison;
        this.entityData.set(DATA_TICKS, this.entangleTicks);
        this.freeze(target);
        if (poison) {
            this.applyPoisonEffect(target);
            if (level instanceof ServerLevel server) {
                com.goetyarcana.combat.VinePoisonTracker.mark(target, (int) server.getGameTime());
            }
        }
    }

/**
     * 结算一次「不可减免」的伤害。
     *
     * <p>先走正常 {@code hurt}（拿死亡结算、掉落、死亡消息），再把被减免掉的部分直接扣血：
     * Goety 的 BOSS 自带的减伤/伤害上限/无敌帧会让 {@code hurt} 只吃进去一点点，
     * 藤毒要求的是一秒比一秒疼、最后能致死，所以差额直接补上。</p>
     */
    private void dealUnstoppable(LivingEntity target, DamageSource source, float damage) {
        float before = target.getHealth();
        target.invulnerableTime = 0;
        target.hurtTime = 0;
        target.hurt(source, damage);
        float lost = before - target.getHealth();
        float rest = damage - Math.max(0.0F, lost);
        if (rest <= 0.01F || !target.isAlive()) {
            return;
        }
        float remaining = target.getHealth() - rest;
        if (remaining <= 0.0F) {
            target.setHealth(0.0F);
            target.die(source);
        } else {
            target.setHealth(remaining);
        }
    }

    /** 先按正常规则加，被免疫挡掉就直接 forceAddEffect（图标/计时仍要显示）。 */
    private void applyPoisonEffect(LivingEntity target) {
        MobEffectInstance instance = new MobEffectInstance(ModEffects.VINE_POISON.get(),
                this.entangleTicks, 0, false, true, true);
        if (!target.addEffect(instance)) {
            target.forceAddEffect(instance, null);
        }
    }

    public int getEntangleTicks() {
        return this.entityData.get(DATA_TICKS);
    }

    public boolean hasPoison() {
        return this.poison;
    }

    private void freeze(LivingEntity target) {
        this.frozenX = target.getX();
        this.frozenY = target.getY();
        this.frozenZ = target.getZ();
        this.frozenYRot = target.getYRot();
        this.frozenXRot = target.getXRot();
        this.hasFrozen = true;
        this.setPos(target.getX(), target.getY(), target.getZ());
        this.entityData.set(DATA_WIDTH, target.getBbWidth());
        this.entityData.set(DATA_HEIGHT, target.getBbHeight());
        if (target instanceof Mob mob && this.previousNoAi == null) {
            this.previousNoAi = mob.isNoAi();
            if (!mob.isNoAi()) {
                mob.setNoAi(true);
            }
        }
    }

    public float getTargetWidth() {
        return this.entityData.get(DATA_WIDTH);
    }

    public float getTargetHeight() {
        return this.entityData.get(DATA_HEIGHT);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            // 客户端只负责活得够久、渲染；位置由服务端同步
            if (++this.life > this.getEntangleTicks()) {
                this.discard();
            }
            return;
        }
        if (++this.life > this.getEntangleTicks()) {
            this.release();
            return;
        }
        LivingEntity target = this.findTarget();
        if (target == null) {
            this.discard();
            return;
        }
        if (!this.hasFrozen) {
            this.freeze(target);
        }

        // 钉住位置与朝向：BOSS 就算无视 noAi 也会被按回来
        target.setDeltaMovement(Vec3.ZERO);
        target.setPos(this.frozenX, this.frozenY, this.frozenZ);
        target.setYRot(this.frozenYRot);
        target.setXRot(this.frozenXRot);
        target.hurtMarked = true;
        this.setPos(this.frozenX, this.frozenY, this.frozenZ);
        this.setDeltaMovement(Vec3.ZERO);

        // 每 tick 只刷新 10 tick；藤蔓消失时会主动清掉
        target.addEffect(new MobEffectInstance(ModEffects.ENTANGLED.get(), 10, 0,
                false, false, false));
        // 藤毒：BOSS 可能清掉负面效果，这里每 tick 补一次；同时刷新禁疗登记
        if (this.poison) {
            // 每 20 tick 强制重挂一次：有些 BOSS 会静默清掉负面效果，只在「没有」时补会漏
            if (!target.hasEffect(ModEffects.VINE_POISON.get()) || this.life % 20 == 0) {
                this.applyPoisonEffect(target);
            }
            if (this.level() instanceof ServerLevel server) {
                int now = (int) server.getGameTime();
                com.goetyarcana.combat.VinePoisonTracker.mark(target, now);
                if (now % 200 == 0) {
                    com.goetyarcana.combat.VinePoisonTracker.prune(now);
                }
            }
        }
        // 禁止瞬移：自家的「扎根」，由 BrambleBindEvents 取消 EntityTeleportEvent
        target.addEffect(new MobEffectInstance(ModEffects.ROOTED.get(), 10, 0, false, false, false));
        // 红刺：缠绕期间每秒扎 2 点伤害
        if (this.life % 20 == 10) {
            target.hurt(this.damageSources().thorns(this), BrambleBindBalance.THORN_DAMAGE);
        }
        // 藤毒：每秒结算一次，伤害随时间递增，绕过护甲/抗性/无敌帧
        if (this.poison && this.life % 20 == 0) {
            int seconds = Math.max(1, this.life / 20);
            float damage = BrambleBindBalance.VINE_POISON_BASE
                    + BrambleBindBalance.VINE_POISON_STEP * (seconds - 1);
            // 红刺刚刚打过会在目标身上留 20 tick 无敌帧，藤毒每隔 20 tick 结算一次，
            // 不清掉的话伤害正好被吃掉（BOSS 的无敌帧更长，更容易全被吞）。这里手动清掉。
            this.dealUnstoppable(target, com.goetyarcana.registry.ModDamageTypes.source(this.level(),
                    com.goetyarcana.registry.ModDamageTypes.VINE_POISON, this, this), damage);
        }
        if (this.level() instanceof ServerLevel server) {
            if (this.life % 5 == 0 && this.life <= this.getEntangleTicks() - DEATH_TICKS) {
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        3, 0.35D, 0.5D, 0.35D, 0.0D);
            }
            // 死亡动画：藤蔓缩回地下，同时散出叶片碎屑
            if (this.life > this.getEntangleTicks() - DEATH_TICKS) {
                float h = target.getBbHeight();
                server.sendParticles(ParticleTypes.COMPOSTER,
                        target.getX(), target.getY() + h * random.nextFloat(), target.getZ(),
                        2, 0.4D, 0.35D, 0.4D, 0.02D);
                if (this.life % 2 == 0) {
                    server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            target.getX(), target.getY() + h * 0.4D, target.getZ(),
                            2, 0.5D, 0.4D, 0.5D, 0.03D);
                }
            }
        }
    }

    public int getLife() {
        return this.life;
    }

    /** 结束：清掉 effect，并把 noAi 还原成缠绕前的值。 */
    private void release() {
        LivingEntity target = this.findTarget();
        if (target != null) {
            target.removeEffect(ModEffects.ENTANGLED.get());
            target.removeEffect(ModEffects.VINE_POISON.get());
            target.removeEffect(ModEffects.ROOTED.get());
            com.goetyarcana.combat.VinePoisonTracker.unmark(target);
            if (this.previousNoAi != null && target instanceof Mob mob) {
                mob.setNoAi(this.previousNoAi);
            }
            this.previousNoAi = null;
        }
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        // 被 /kill、区块卸载等外部原因移除时也要还原
        if (!this.level().isClientSide && this.previousNoAi != null) {
            LivingEntity target = this.findTarget();
            if (target != null) {
                target.removeEffect(ModEffects.ENTANGLED.get());
                target.removeEffect(ModEffects.VINE_POISON.get());
                target.removeEffect(ModEffects.ROOTED.get());
                com.goetyarcana.combat.VinePoisonTracker.unmark(target);
                if (target instanceof Mob mob) {
                    mob.setNoAi(this.previousNoAi);
                }
            }
            this.previousNoAi = null;
        }
        super.remove(reason);
    }

    private LivingEntity findTarget() {
        if (!(this.level() instanceof ServerLevel server) || this.targetId < 0) {
            return null;
        }
        Entity entity = server.getEntity(this.targetId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_WIDTH, 0.6F);
        this.entityData.define(DATA_HEIGHT, 1.95F);
        this.entityData.define(DATA_TICKS, BrambleBindBalance.ENTANGLE_TICKS);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.life = tag.getInt("Life");
        this.targetId = tag.getInt("Target");
        this.entangleTicks = tag.contains("Ticks") ? tag.getInt("Ticks") : BrambleBindBalance.ENTANGLE_TICKS;
        this.poison = tag.getBoolean("Poison");
        this.entityData.set(DATA_TICKS, this.entangleTicks);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", this.life);
        tag.putInt("Target", this.targetId);
        tag.putInt("Ticks", this.entangleTicks);
        tag.putBoolean("Poison", this.poison);
    }
}
