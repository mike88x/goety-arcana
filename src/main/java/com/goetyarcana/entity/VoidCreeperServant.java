package com.goetyarcana.entity;

import com.goetyarcana.VoidBlastBalance;
import com.goetyarcana.combat.VoidBlastCurseTracker;
import com.goetyarcana.combat.VoidBlastTags;
import com.goetyarcana.registry.ModEffects;
import com.goetyarcana.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 虚空苦力怕仆从。
 *
 * <p>血量 10 / 护甲 6 / 韧性 6 / 索敌 20 格。看到敌人后先「瞬移准备」2 秒，
 * 瞬移到目标旁边，再 0.3 秒后爆炸，造成原版苦力怕困难模式伤害的 50%。
 * 活下来的目标会被施加砰砰诅咒；没被炸死的普通怪生命上限被压到 1，
 * 精英 / BOSS 则变成下一次爆炸 +50%（10 分钟）。</p>
 *
 * <p>它还会强制吸引周边敌对生物的火力（即使它们原本锁定玩家）。</p>
 */
public class VoidCreeperServant extends com.Polarice3.Goety.common.entities.ally.Summoned {

    /** 0 = 待机/跟随，1 = 瞬移准备，2 = 已就位待爆。 */
    private static final String TAG_STATE = "State";
    private static final String TAG_TIMER = "Timer";
    private static final String TAG_OWNER = "Owner";

    private int state;
    private int timer;
    private boolean exploded;

    /** 客户端也需要的状态：0 待机 / 1 冲锋 / 2 引信。 */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_STATE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(VoidCreeperServant.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_FUSE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(VoidCreeperServant.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);
    private int tauntCooldown;
    private int headLookCooldown = 40;
    @Nullable
    private UUID ownerId;

    public VoidCreeperServant(EntityType<? extends VoidCreeperServant> type, Level level) {
        super(type, level);
    }

    public VoidCreeperServant(Level level, LivingEntity owner) {
        this(ModEntities.VOID_CREEPER_SERVANT.get(), level);
        this.setOwnerId(owner.getUUID());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STATE, 0);
        this.entityData.define(DATA_FUSE, 0);
    }

    public int getServantState() {
        return this.entityData.get(DATA_STATE);
    }

    public int getFuseLeft() {
        return this.entityData.get(DATA_FUSE);
    }

    private void setState(int state) {
        this.state = state;
        this.entityData.set(DATA_STATE, state);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VoidBlastBalance.SERVANT_HEALTH)
                .add(Attributes.ARMOR, VoidBlastBalance.SERVANT_ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, VoidBlastBalance.SERVANT_TOUGHNESS)
                .add(Attributes.FOLLOW_RANGE, VoidBlastBalance.SERVANT_RANGE)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    // ---------------------------------------------------------------- 仆从归属

    private boolean isOwnedBy(Entity entity) {
        return entity instanceof com.Polarice3.Goety.api.entities.IOwned owned
                && owned.getTrueOwner() == this.getTrueOwner() && this.getTrueOwner() != null;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        LivingEntity owner = this.getTrueOwner();
        if (owner != null && (other == owner || this.isOwnedBy(other))) {
            return true;
        }
        return super.isAlliedTo(other);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // 瞬移 + 爆炸这一段强制无敌，任何伤害都不吃
        if (this.state != 0) {
            return false;
        }
        return super.hurt(source, amount);
    }

    // ---------------------------------------------------------------- 行为

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = this.getTrueOwner();
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }
        if (this.tickCount % 200 == 0 && this.level() instanceof ServerLevel server) {
            VoidBlastCurseTracker.prune((int) server.getGameTime());
        }

        switch (this.state) {
            case 0 -> this.idleTick(owner);
            case 1 -> this.windupTick();
            case 2 -> this.fuseTick(owner);
            default -> this.setState(0);
        }
    }

    private static final boolean DEBUG = false;

    private void debug(String message) {
        if (DEBUG) {
            com.goetyarcana.GoetyArcanaMod.LOGGER.info("[void_creeper] {}", message);
        }
    }

    private void idleTick(LivingEntity owner) {
        // 离主人太远就追上去（瞬移过去，免得跟不上）
        if (this.distanceToSqr(owner) > 24.0D * 24.0D) {
            this.teleportNear(owner);
        } else if (this.distanceToSqr(owner) > 12.0D * 12.0D) {
            this.getNavigation().moveTo(owner, 1.2D);
        } else {
            this.getNavigation().stop();
        }

        // 待机时偶尔转个头，像在四处张望
        if (--this.headLookCooldown <= 0) {
            this.headLookCooldown = 140 + this.random.nextInt(140);
            this.setYHeadRot(this.getYRot() + (this.random.nextFloat() - 0.5F) * 80.0F);
        }
        if (--this.tauntCooldown <= 0) {
            this.taunt();
            this.tauntCooldown = VoidBlastBalance.TAUNT_INTERVAL;
        }

        LivingEntity target = this.findTarget();
        if (target != null) {
            this.debug("target=" + target.getType() + " dist=" + (int) Math.sqrt(this.distanceToSqr(target)));
            this.setState(1);
            this.setInvulnerable(true);
            this.timer = VoidBlastBalance.TELEPORT_WINDUP;
            this.getNavigation().stop();
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY() + 0.6D, this.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.1D);
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }

    private void windupTick() {
        // 头/身体对准目标，并往前冲一小段（纯视觉，真正的位移靠最后的瞬移）
        LivingEntity target = this.findTarget();
        if (target != null) {
            this.getLookControl().setLookAt(target, 40.0F, 40.0F);
            this.setYHeadRot((float) (Math.toDegrees(Math.atan2(
                    target.getZ() - this.getZ(), target.getX() - this.getX())) - 90.0D));
            Vec3 dash = new Vec3(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
            if (dash.lengthSqr() > 1.0E-4D) {
                dash = dash.normalize().scale(0.16D);
                this.setDeltaMovement(dash.x, this.getDeltaMovement().y, dash.z);
                this.hurtMarked = true;
            }
        } else {
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        }
        this.getNavigation().stop();
        if (this.level() instanceof ServerLevel server) {
            // 冲锋拖尾：虚空火 + 反向传送门 + 灵魂火
            server.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY() + 0.6D, this.getZ(), 3, 0.25D, 0.35D, 0.25D, 0.04D);
            if (this.timer % 3 == 0) {
                server.sendParticles(com.goetyarcana.registry.ModParticles.VOID_FLAME.get(),
                        this.getX(), this.getY() + 0.5D, this.getZ(), 2, 0.2D, 0.25D, 0.2D, 0.01D);
                server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + 0.3D, this.getZ(), 1, 0.2D, 0.2D, 0.2D, 0.01D);
            }
        }
        if (--this.timer > 0) {
            return;
        }
        LivingEntity locked = this.findTarget();
        if (locked == null) {
            this.setState(0);
            this.setInvulnerable(false);
            return;
        }
        this.teleportNextTo(locked);
        this.setState(2);
        this.timer = VoidBlastBalance.FUSE_TICKS;
        this.level().playSound(null, this.blockPosition(),
                SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    private void fuseTick(LivingEntity owner) {
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        this.entityData.set(DATA_FUSE, Math.max(0, this.timer));
        if (this.level() instanceof ServerLevel server) {
            float progress = 1.0F - Math.max(0, this.timer) / (float) VoidBlastBalance.FUSE_TICKS;
            int count = 1 + (int) (progress * 4);
            server.sendParticles(com.goetyarcana.registry.ModParticles.VOID_FLAME.get(),
                    this.getX(), this.getY() + 0.9D, this.getZ(), count, 0.3D, 0.5D, 0.3D, 0.02D);
        }
        if (--this.timer > 0) {
            return;
        }
        this.explode(owner);
    }

    private void teleportNear(Entity destination) {
        this.teleportNextTo(destination);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY() + 0.6D, this.getZ(), 20, 0.3D, 0.5D, 0.3D, 0.1D);
        }
    }

    private void teleportNextTo(Entity target) {
        Vec3 offset = new Vec3(this.random.nextDouble() - 0.5D, 0.0D, this.random.nextDouble() - 0.5D)
                .normalize().scale(1.2D);
        double x = target.getX() + offset.x * 0.3D;
        double z = target.getZ() + offset.z * 0.3D;
        // 先贴到目标脚下，再按实际碰撞盒一步步往上抬，避免落进方块里
        double y = target.getY();
        this.teleportTo(x, y, z);
        for (int i = 0; i < 12 && !this.level().noCollision(this, this.getBoundingBox()); i++) {
            this.teleportTo(x, y + 0.2D * (i + 1), z);
        }
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    target.getX(), target.getY() + 0.6D, target.getZ(), 30, 0.5D, 0.7D, 0.5D, 0.1D);
        }
    }

    private boolean canStandAt(double x, double y, double z) {
        net.minecraft.world.phys.AABB box = this.getType().getDimensions().makeBoundingBox(x, y, z);
        return this.level().noCollision(this, box);
    }

    /** 敌对判定：Enemy / Monster / MONSTER 分类都算（Goety 的末影遣使就是 Enemy 而非 Monster）。 */
    private boolean isHostileMob(LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.monster.Enemy
                || entity instanceof Monster
                || entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER;
    }

    /** 强制吸引火力：把周围敌对生物的目标改成自己。 */
    private void taunt() {
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }
        AABB box = this.getBoundingBox().inflate(VoidBlastBalance.TAUNT_RANGE);
        List<Mob> mobs = this.level().getEntitiesOfClass(Mob.class, box,
                mob -> mob != this && mob.isAlive() && this.isHostileMob(mob) && !this.isAlliedTo(mob));
        for (Mob mob : mobs) {
            if (mob.getTarget() != this) {
                mob.setTarget(this);
            }
        }
    }

    @Nullable
    private LivingEntity findTarget() {
        AABB box = this.getBoundingBox().inflate(VoidBlastBalance.SERVANT_RANGE);
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != this
                        && entity.isAlive()
                        && this.isHostileMob(entity)
                        && !this.isAlliedTo(entity)
                        && !(entity instanceof VoidCreeperServant));
        candidates.sort(Comparator.comparingDouble(this::distanceToSqr));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    // ---------------------------------------------------------------- 爆炸

    private void explode(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel server) || this.exploded) {
            return;
        }
        this.exploded = true;
        int now = (int) server.getGameTime();
        double radius = VoidBlastBalance.EXPLOSION_RADIUS;
        AABB box = this.getBoundingBox().inflate(radius);

        List<LivingEntity> victims = server.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != this && entity.isAlive() && !this.isAlliedTo(entity));
        this.debug("explode at " + this.blockPosition() + " victims=" + victims.size());
        float dealtTotal = 0.0F;
        for (LivingEntity victim : victims) {
            double distance = Math.sqrt(victim.getBoundingBox().distanceToSqr(
                    this.position().add(0.0D, 0.5D, 0.0D)));
            if (distance > radius) {
                continue;
            }
            double factor = 1.0D - (distance / radius) * (1.0D - VoidBlastBalance.EXPLOSION_MIN_FACTOR);
            float bonus = VoidBlastCurseTracker.consume(victim, now);
            float damage = (float) (this.explosionDamage() * factor * (1.0D + bonus));
            net.minecraft.world.damagesource.DamageSource source =
                    this.damageSources().explosion(this, owner);
            float before = victim.getHealth();
            boolean hit = victim.hurt(source, damage);
            dealtTotal += Math.max(0.0F, before - victim.getHealth());
            float rest = damage - Math.max(0.0F, before - victim.getHealth());
            if (rest > 0.01F && victim.isAlive()) {
                // BOSS 的限伤会把伤害砍掉，剩的差额直接扣血
                victim.setHealth(Math.max(0.0F, victim.getHealth() - rest));
                dealtTotal += rest;
                if (victim.getHealth() <= 0.0F) {
                    victim.die(source);
                }
            }
            this.debug("victim=" + victim.getType() + " dmg=" + damage + " bonus=" + bonus
                    + " hit=" + hit + " elite=" + VoidBlastTags.isEliteOrBoss(victim));

            if (!victim.isAlive() || victim.isDeadOrDying()) {
                continue;
            }
            this.applyCurse(victim, now);
        }

        // 虚空套：自爆把灵魂带回给主人（造成伤害的 125%）
        returnSoul(owner, dealtTotal);

        this.clearAggro(server);
        com.goetyarcana.combat.VoidBlastVisuals.explode(server,
                this.getX(), this.getY() + 0.5D, this.getZ());
        this.discard();
    }

    /** 虚空套：自爆仆从把造成伤害的 125% 作为灵魂能量还给主人。 */
    private void returnSoul(LivingEntity owner, float dealt) {
        if (!(owner instanceof net.minecraft.world.entity.player.Player player) || dealt <= 0.0F) {
            return;
        }
        if (!com.goetyarcana.event.VoidArmorEvents.isFullSet(player)) {
            return;
        }
        int gain = Math.max(1, Math.round(dealt * 1.25F));
        player.getCapability(
                        com.Polarice3.Goety.common.capabilities.soulenergy.SEProvider.CAPABILITY)
                .ifPresent(energy -> energy.increaseSE(gain));
        this.debug("soul returned=" + gain);
    }

    /** 爆炸伤害：附魔版覆写成闪电苦力怕的 75%。 */
    protected float explosionDamage() {
        return VoidBlastBalance.EXPLOSION_DAMAGE;
    }

    /** 诅咒给精英 / BOSS 的加成。 */
    protected float curseBonus() {
        return VoidBlastBalance.CURSE_BLAST_BONUS;
    }

    /** 诅咒的 amplifier：0 = 一级，1 = 二级（附魔版）。 */
    protected int curseAmplifier() {
        return 0;
    }

    /** 诅咒时长：普通怪 1 分钟，精英 / BOSS 10 分钟。 */
    protected int curseTicksFor(LivingEntity victim) {
        return VoidBlastTags.isEliteOrBoss(victim)
                ? VoidBlastBalance.CURSE_TICKS
                : VoidBlastBalance.CURSE_TICKS_NORMAL;
    }

    /** 活下来的目标：普通怪压到 1 血上限，精英 / BOSS 记一层「下次爆炸加成」。 */
    private void applyCurse(LivingEntity victim, int now) {
        this.debug("curse " + victim.getType() + " elite=" + VoidBlastTags.isEliteOrBoss(victim));
        MobEffectInstance existing = victim.getEffect(ModEffects.VOID_BLAST_CURSE.get());
        if (existing != null) {
            // 二级诅咒不能被后来的一级覆盖；同级也不刷新成更短的时长
            if (existing.getAmplifier() > this.curseAmplifier()) {
                return;
            }
            if (existing.getAmplifier() == this.curseAmplifier()
                    && existing.getDuration() >= this.curseTicksFor(victim)) {
                return;
            }
        }
        MobEffectInstance instance = new MobEffectInstance(ModEffects.VOID_BLAST_CURSE.get(),
                this.curseTicksFor(victim), this.curseAmplifier(), false, true, true);
        boolean added = victim.addEffect(instance);
        if (!added) {
            victim.forceAddEffect(instance, null);
        }
        this.debug("curse apply added=" + added + " hasEffect="
                + victim.hasEffect(ModEffects.VOID_BLAST_CURSE.get()));
        if (VoidBlastTags.isEliteOrBoss(victim)) {
            VoidBlastCurseTracker.mark(victim, now, this.curseTicksFor(victim), this.curseBonus());
            if (this.level() instanceof ServerLevel server) {
                com.goetyarcana.network.ModNetwork.sendVoidCurse(server, victim,
                        this.curseTicksFor(victim));
            }
        }
    }

    /** 爆炸/死亡后，附近还锁着我们的怪物把目标清掉，避免对着空气挥刀。 */
    private void clearAggro(ServerLevel server) {
        net.minecraft.world.phys.AABB box = this.getBoundingBox().inflate(48.0D);
        for (net.minecraft.world.entity.Mob mob : server.getEntitiesOfClass(
                net.minecraft.world.entity.Mob.class, box, mob -> mob != this)) {
            if (mob.getTarget() == this) {
                mob.setTarget(null);
            }
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        LivingEntity owner = this.getTrueOwner();
        if (!this.level().isClientSide && !this.exploded) {
            this.explode(owner);
        }
        super.die(source);
    }

    // ---------------------------------------------------------------- 存档

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_STATE, this.state);
        tag.putInt(TAG_TIMER, this.timer);
        if (this.ownerId != null) {
            tag.putUUID(TAG_OWNER, this.ownerId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.state = tag.getInt(TAG_STATE);
        this.timer = tag.getInt(TAG_TIMER);
        if (tag.hasUUID(TAG_OWNER)) {
            this.ownerId = tag.getUUID(TAG_OWNER);
        }
    }
}
