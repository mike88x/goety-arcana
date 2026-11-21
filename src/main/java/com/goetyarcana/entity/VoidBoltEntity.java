package com.goetyarcana.entity;

import com.goetyarcana.registry.ModEffects;
import com.goetyarcana.registry.ModEntities;
import com.goetyarcana.registry.ModItems;
import com.goetyarcana.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * 虚空弩矢：飞行/插在目标上 4 秒后爆炸，爆炸前箭头持续闪烁。
 *
 * <p>射击者穿着虚空长袍时（见 {@code VoidRobe}）进入「追踪」模式：
 * 自动朝前方最近的敌人转向，命中不消失而是继续穿透，最多穿透
 * {@value #MAX_PIERCES} 个目标，达到上限时在命中处直接爆炸。
 * 追踪模式下不再给目标挂 {@code void_explosion}，否则一次射击会连爆一串。</p>
 */
public class VoidBoltEntity extends AbstractArrow {

    private static final int FUSE_TICKS = 80;
    /** 追踪模式下引信放长：要留时间绕去下一个目标。 */
    private static final int FUSE_TICKS_HOMING = 140;
    /** 追踪模式最多穿透的敌人数；到上限当场爆炸。 */
    private static final int MAX_PIERCES = 6;
    /** 追踪索敌半径。 */
    private static final double TRACK_RANGE = 26.0D;
    /** 每 tick 把速度方向朝目标拧多少（0~1，越大越像导弹）。 */
    private static final double TRACK_TURN = 0.5D;
    /** 只追当前飞行方向前方这个点积以外的目标（-0.2 ≈ 100° 锥角，允许稍微偏后）。 */
    private static final double TRACK_CONE = -0.2D;
    private static final int RETARGET_INTERVAL = 4;

    private int fuse;
    private LivingEntity stuckEntity;
    private boolean stuck;
    private Vec3 stuckPos = Vec3.ZERO;
    private Vec3 stuckOffset = Vec3.ZERO;
    private Vec3 heading = Vec3.ZERO;
    private Vec3 stuckHeading = Vec3.ZERO;

    /** 追踪 + 穿透模式（虚空长袍）。 */
    private boolean homing;
    private int pierces;
    private final Set<Integer> piercedIds = new HashSet<>();
    @Nullable
    private LivingEntity trackTarget;
    private int retargetTimer;

    public VoidBoltEntity(EntityType<? extends VoidBoltEntity> type, Level level) {
        super(type, level);
        this.setBaseDamage(6.0D);
        this.pickup = AbstractArrow.Pickup.DISALLOWED;
    }

    public VoidBoltEntity(Level level, LivingEntity owner) {
        this(ModEntities.VOID_BOLT.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
    }

    /** 由 {@code VoidBoltItem#createArrow} 在射击者穿虚空长袍时调用。 */
    public void enableVoidRobeTracking() {
        this.homing = true;
    }

    public boolean isHoming() {
        return this.homing;
    }

    public int getPierces() {
        return this.pierces;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            ServerLevel server = (ServerLevel) this.level();
            if (this.fuse % 2 == 0) {
                server.sendParticles(ModParticles.VOID_FLAME.get(),
                        this.getX(), this.getY(), this.getZ(), 2, 0.04D, 0.04D, 0.04D, 0.01D);
                double angle = this.fuse * 0.45D;
                server.sendParticles(ModParticles.VOID_FLAME.get(),
                        this.getX() + Math.cos(angle) * 0.28D,
                        this.getY() + 0.1D,
                        this.getZ() + Math.sin(angle) * 0.28D,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            if (this.fuse % 12 == 0) {
                server.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getY(), this.getZ(), 6, 0.12D, 0.12D, 0.12D, 0.02D);
            }
            if (this.stuck && this.fuse % 8 == 0) {
                Vec3 tip = this.position().add(this.stuckHeading.scale(0.35D));
                server.sendParticles(ModParticles.VOID_FLAME.get(), tip.x, tip.y, tip.z,
                        1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
        }
        if (!this.level().isClientSide
                && ++this.fuse >= (this.homing ? FUSE_TICKS_HOMING : FUSE_TICKS)) {
            this.explode();
            return;
        }
        if (stuckEntity != null && stuckEntity.isAlive()) {
            this.setPos(stuckEntity.getX() + stuckOffset.x,
                    stuckEntity.getY() + stuckEntity.getBbHeight() * 0.62D + stuckOffset.y,
                    stuckEntity.getZ() + stuckOffset.z);
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (this.stuck) {
            this.setPos(stuckPos.x, stuckPos.y, stuckPos.z);
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (!this.level().isClientSide && this.homing && !this.inGround) {
            this.steerToTarget();
        }
        Vec3 from = this.position();
        super.tick();
        if (!this.level().isClientSide && this.homing && !this.inGround && !this.isRemoved()) {
            this.sweepPierce(from, this.position());
        }
    }

    /**
     * 自己扫一遍这一 tick 走过的线段。
     *
     * <p>箭速能到 2.5 格/tick，原版每 tick 只回报最近的<b>一个</b>碰撞结果：
     * 一 tick 内横穿两只怪时后一只会被漏掉，箭也会直接隧穿过去。
     * 所以穿透模式下把 from→to 这段线段上的所有目标都结算一遍。</p>
     */
    private void sweepPierce(Vec3 from, Vec3 to) {
        double lengthSqr = from.distanceToSqr(to);
        if (lengthSqr < 1.0E-6D) {
            return;
        }
        var box = new AABB(from, to).inflate(0.6D);
        for (LivingEntity candidate : this.level().getEntitiesOfClass(LivingEntity.class, box, this::canTrack)) {
            if (candidate.getBoundingBox().inflate(0.35D).clip(from, to).isPresent()) {
                this.pierceThrough(candidate);
                if (this.isRemoved()) {
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 追踪
    // ------------------------------------------------------------------

    private void steerToTarget() {
        if (this.trackTarget == null || !this.trackTarget.isAlive()
                || this.trackTarget.level() != this.level()
                || this.piercedIds.contains(this.trackTarget.getId())
                || this.trackTarget.distanceToSqr(this) > TRACK_RANGE * TRACK_RANGE
                || --this.retargetTimer <= 0) {
            this.trackTarget = this.findTrackTarget();
            this.retargetTimer = RETARGET_INTERVAL;
        }
        LivingEntity target = this.trackTarget;
        if (target == null) {
            return;
        }

        Vec3 aim = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ())
                .subtract(this.position());
        double distance = aim.length();
        if (distance < 0.4D) {
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        double speed = Math.max(0.85D, motion.length());

        // 提前量：目标在飞过去这段时间里会走多远，打移动目标才不会擦肩而过。
        double flightTicks = Math.min(20.0D, distance / speed);
        Vec3 lead = target.getDeltaMovement().scale(flightTicks * 0.6D);
        Vec3 desired = aim.add(lead).normalize();

        Vec3 steered = motion.lengthSqr() > 1.0E-6D
                ? motion.normalize().lerp(desired, TRACK_TURN)
                : desired;
        if (steered.lengthSqr() < 1.0E-6D) {
            steered = desired;
        }
        this.setDeltaMovement(steered.normalize().scale(speed));
        this.faceMotion();
    }

    @Nullable
    private LivingEntity findTrackTarget() {
        Vec3 heading = this.getDeltaMovement();
        return com.goetyarcana.combat.Homing.bestTarget(this, this.getOwner(),
                this.position(), heading, TRACK_RANGE, TRACK_CONE, this::canTrack);
    }

    private boolean canTrack(LivingEntity candidate) {
        if (candidate == this.getOwner() || !candidate.isAlive() || candidate.isSpectator()) {
            return false;
        }
        if (candidate instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return this.canHitEntity(candidate);
    }

    /** 已经穿过的目标不再参与碰撞，避免箭在尸体里反复“命中”而卡住。 */
    @Override
    protected boolean canHitEntity(Entity candidate) {
        return super.canHitEntity(candidate)
                && (!this.homing || !this.piercedIds.contains(candidate.getId()));
    }

    // ------------------------------------------------------------------
    // 命中
    // ------------------------------------------------------------------

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (!(target instanceof LivingEntity living) || living == this.getOwner()) {
            super.onHitEntity(result);
            return;
        }
        if (this.homing) {
            this.pierceThrough(living);
            return;
        }
        this.saveHeading();
        if (living instanceof EnderMan) {
            // 无 owner 的魔法伤害，避免末影人 hurt() 的传送判定。
            living.hurt(this.damageSources().magic(), (float) this.getBaseDamage());
        } else {
            super.onHitEntity(result);
        }
        living.addEffect(new MobEffectInstance(ModEffects.VOID_EXPLOSION.get(),
                80, 0, false, true, true));
        if (this.getOwner() instanceof net.minecraft.world.entity.player.Player player) {
            com.goetyarcana.event.VoidCrossbowEvents.addEyeStack(player, living);
        }
        this.discard();
    }

    /** 追踪模式：伤害该目标、记一次穿透，穿满上限就当场爆炸。 */
    private void pierceThrough(LivingEntity living) {
        if (!this.piercedIds.add(living.getId())) {
            return;
        }
        this.saveHeading();
        this.dealPierceDamage(living);
        if (this.getOwner() instanceof net.minecraft.world.entity.player.Player player) {
            com.goetyarcana.event.VoidCrossbowEvents.addEyeStack(player, living);
        }
        this.pierces++;

        if (this.pierces >= MAX_PIERCES) {
            this.explode();
            return;
        }

        // 刚穿过的目标不能再当追踪目标，否则箭会绕回去原地打转。
        if (this.trackTarget == living) {
            this.trackTarget = null;
        }
        this.retargetTimer = 0;

        // 穿透后保持前进，别被实体碰撞挤停。
        if (this.getDeltaMovement().lengthSqr() < 1.0E-4D) {
            Vec3 fallback = this.heading.lengthSqr() > 1.0E-6D
                    ? this.heading
                    : new Vec3(0.0D, 0.0D, 1.0D);
            this.setDeltaMovement(fallback.scale(1.2D));
        }
    }

    private void dealPierceDamage(LivingEntity living) {
        float speed = (float) this.getDeltaMovement().length();
        float damage = (float) Mth.clamp(Math.ceil(speed * this.getBaseDamage()), 1.0D, 40.0D);
        boolean hurt = living instanceof EnderMan
                ? living.hurt(this.damageSources().magic(), damage)
                : living.hurt(this.damageSources().arrow(this, this.getOwner()), damage);

        if (!this.level().isClientSide) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.9F, 1.15F);
            if (this.level() instanceof ServerLevel server) {
                server.sendParticles(ModParticles.VOID_FLAME.get(),
                        this.getX(), this.getY(), this.getZ(), hurt ? 10 : 4,
                        0.2D, 0.2D, 0.2D, 0.04D);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        this.saveHeading();
        super.onHitBlock(result);
        this.stuck = true;
        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
        this.stuckHeading = normal.scale(-1.0D);
        this.stuckPos = result.getLocation().add(normal.scale(0.55D));
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
    }

    // ------------------------------------------------------------------
    // 杂项
    // ------------------------------------------------------------------

    private void saveHeading() {
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-5D) {
            this.heading = motion.normalize();
            this.faceMotion();
        }
    }

    private void faceMotion() {
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() <= 1.0E-5D) {
            return;
        }
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.setYRot((float) (Mth.atan2(motion.x, motion.z) * (180.0D / Math.PI)));
        this.setXRot((float) (-Mth.atan2(motion.y, horizontal) * (180.0D / Math.PI)));
    }

    private void explode() {
        if (this.level().isClientSide) {
            this.discard();
            return;
        }
        com.goetyarcana.combat.VoidExplosion.explode(this.level(), this.position(), this.getOwner());
        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Homing", this.homing);
        tag.putInt("Pierces", this.pierces);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.homing = tag.getBoolean("Homing");
        this.pierces = tag.getInt("Pierces");
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ModItems.VOID_CROSSBOW_BOLT.get());
    }

    public int getFuse() {
        return this.fuse;
    }

    public boolean isInGround() {
        return this.stuck || this.inGround;
    }

    public Vec3 getStuckHeading() {
        return this.stuckHeading;
    }

    public boolean isStuckInEntity() {
        return this.stuckEntity != null && this.stuckEntity.isAlive();
    }
}
