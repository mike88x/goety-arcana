package com.goetyarcana.entity;

import com.goetyarcana.VoidScytheBalance;
import com.goetyarcana.combat.CombatUtil;
import com.goetyarcana.effect.ExecutionEffect;
import com.goetyarcana.registry.ModDamageTypes;
import com.goetyarcana.registry.ModEntities;
import com.goetyarcana.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 虚空镰刀的剑气。
 *
 * <p>与死神之镰（Goety 的 {@code ScytheSlash}）一样，这是一道<b>向前飞行</b>的
 * 能量斩击，而不是以玩家为中心扩散的扇形：</p>
 * <ul>
 *   <li>沿玩家视线飞行，速度 {@link VoidScytheBalance#WAVE_SPEED} 格/tick；</li>
 *   <li><b>横向剑气</b>：刃面垂直于飞行方向、横向铺开，渲染时跟随实体朝向；</li>
 *   <li><b>平滑运动</b>：起飞 4 tick 平滑加速、寿命最后 12 tick 平滑减速，
 *       位置与朝向都交给客户端做 tick 间插值；</li>
 *   <li>持续 {@link VoidScytheBalance#WAVE_LIFETIME} tick（100 tick = 5 秒）；</li>
 *   <li>不碰撞方块，可以穿墙，直到寿命结束才消散；</li>
 *   <li>穿透生物：每个目标只结算一次，剑气命中后继续向前飞；</li>
 *   <li>伤害仍然是目标最大生命值的 5%~15%（按蓄力比例），并保留 5% 处决。</li>
 * </ul>
 *
 * <p>命中判定使用“上一 tick 位置 + 当前 tick 位置”的扫掠 AABB，
 * 即使剑气每 tick 前进 1 格、目标站在两帧之间也不会被漏掉。</p>
 */
public class ScytheWaveEntity extends Projectile {

    private static final EntityDataAccessor<Float> DATA_CHARGE =
            SynchedEntityData.defineId(ScytheWaveEntity.class, EntityDataSerializers.FLOAT);

    /** 已经结算过的目标；穿透时避免同一目标每 tick 重复掉血。 */
    private final Set<UUID> hitEntities = new HashSet<>();

    /** 只在整个剑气生命周期内播放一次命中音效，避免穿透一群怪时刷屏。 */
    private boolean playedHitSound;

    public ScytheWaveEntity(EntityType<? extends ScytheWaveEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ScytheWaveEntity(Level level, Player owner, float charge, Vec3 direction) {
        this(ModEntities.SCYTHE_WAVE.get(), level);
        this.setOwner(owner);

        Vec3 dir = direction.lengthSqr() < 1.0E-6D
                ? Vec3.directionFromRotation(owner.getXRot(), owner.getYRot())
                : direction.normalize();
        Vec3 spawn = owner.getEyePosition()
                .add(dir.scale(VoidScytheBalance.WAVE_SPAWN_OFFSET))
                .add(0.0D, -0.15D, 0.0D);

        this.setPos(spawn.x, spawn.y, spawn.z);
        this.setDeltaMovement(dir.scale(VoidScytheBalance.WAVE_SPEED));
        this.setYRot(yawOf(dir));
        this.setXRot(pitchOf(dir));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setCharge(charge);
    }

    @Override
    protected void defineSynchedData() {
        // Entity#defineSynchedData 在 1.20.1 是抽象方法，不存在可调用的 super 实现。
        this.entityData.define(DATA_CHARGE, 0.0F);
    }

    public float getCharge() {
        return this.entityData.get(DATA_CHARGE);
    }

    public void setCharge(float charge) {
        this.entityData.set(DATA_CHARGE, Mth.clamp(charge, 0.0F, 1.0F));
    }

    /** 剑气横向宽度（格），随蓄力提升。 */
    public double getWaveWidth() {
        return Mth.lerp(this.getCharge(),
                VoidScytheBalance.WAVE_MIN_WIDTH,
                VoidScytheBalance.WAVE_MAX_WIDTH);
    }

    /** 剑气纵向高度（格），随蓄力提升。 */
    public double getWaveHeight() {
        return Mth.lerp(this.getCharge(),
                VoidScytheBalance.WAVE_MIN_HEIGHT,
                VoidScytheBalance.WAVE_MAX_HEIGHT);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            this.clientTrailParticles();
            return;
        }

        if (this.tickCount >= VoidScytheBalance.WAVE_LIFETIME) {
            this.finish();
            return;
        }

        Vec3 movement = this.getDeltaMovement();
        Vec3 direction = movement.lengthSqr() < 1.0E-6D
                // 极端情况下（被其它模组改了速度），按朝向继续飞，避免剑气原地不动。
                ? Vec3.directionFromRotation(this.getXRot(), this.getYRot())
                : movement.normalize();

        // 平滑运动：起飞时从半速加速到全速，寿命末尾再平滑减速，
        // 而不是凭空出现后一直匀速硬邦邦地平移。
        double speed = VoidScytheBalance.WAVE_SPEED * speedScale(this.tickCount);
        Vec3 velocity = direction.scale(speed);
        this.setDeltaMovement(velocity);

        Vec3 from = this.position();
        this.setPos(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
        Vec3 to = this.position();

        // 让实体朝向平滑跟随运动方向：客户端用 yRotO/xRotO 做插值，渲染不会跳帧。
        ProjectileUtil.rotateTowardsMovement(this, 0.5F);

        this.damageTargets(from, to);
    }

    /**
     * 剑气当前 tick 的速度倍率。
     *
     * <p>起飞 {@link VoidScytheBalance#WAVE_LAUNCH_TICKS} tick 内用 smoothstep
     * 从 {@link VoidScytheBalance#WAVE_LAUNCH_SPEED} 升到 1.0；寿命最后
     * {@link VoidScytheBalance#WAVE_DISSIPATE_TICKS} tick 再平滑降到
     * {@link VoidScytheBalance#WAVE_END_SPEED}。</p>
     */
    private static double speedScale(int tick) {
        double factor = 1.0D;

        if (VoidScytheBalance.WAVE_LAUNCH_TICKS > 0) {
            double launch = Mth.clamp((tick + 1.0D) / VoidScytheBalance.WAVE_LAUNCH_TICKS,
                    0.0D, 1.0D);
            launch = launch * launch * (3.0D - 2.0D * launch);
            factor = VoidScytheBalance.WAVE_LAUNCH_SPEED
                    + (1.0D - VoidScytheBalance.WAVE_LAUNCH_SPEED) * launch;
        }

        int remaining = VoidScytheBalance.WAVE_LIFETIME - tick;
        if (remaining < VoidScytheBalance.WAVE_DISSIPATE_TICKS) {
            double fade = Mth.clamp(remaining / (double) VoidScytheBalance.WAVE_DISSIPATE_TICKS,
                    0.0D, 1.0D);
            fade = fade * fade * (3.0D - 2.0D * fade);
            factor *= VoidScytheBalance.WAVE_END_SPEED
                    + (1.0D - VoidScytheBalance.WAVE_END_SPEED) * fade;
        }

        return factor;
    }

    private void damageTargets(Vec3 from, Vec3 to) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        Entity owner = this.getOwner();
        Player ownerPlayer = owner instanceof Player player ? player : null;

        // 命中盒沿飞行方向取 from→to 的并集，再按剑气宽度/高度向四周扩展。
        double halfWidth = this.getWaveWidth() * 0.5D;
        double halfHeight = this.getWaveHeight() * 0.5D;
        double halfDepth = VoidScytheBalance.WAVE_HIT_DEPTH * 0.5D;
        AABB area = new AABB(
                Math.min(from.x, to.x) - halfWidth,
                Math.min(from.y, to.y) - halfHeight,
                Math.min(from.z, to.z) - halfWidth,
                Math.max(from.x, to.x) + halfWidth,
                Math.max(from.y, to.y) + halfHeight,
                Math.max(from.z, to.z) + halfWidth)
                .inflate(halfDepth);

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, area,
                target -> CombatUtil.isValidTarget(ownerPlayer, target));

        DamageSource source = ModDamageTypes.source(level,
                ModDamageTypes.VOID_WAVE, this, owner);
        float percent = Mth.lerp(this.getCharge(),
                VoidScytheBalance.WAVE_MIN_PERCENT,
                VoidScytheBalance.WAVE_MAX_PERCENT);

        for (LivingEntity target : targets) {
            UUID uuid = target.getUUID();
            if (!this.hitEntities.add(uuid)) {
                continue;
            }

            // 剑气没有基础伤害，只有目标最大生命值百分比。
            float damage = target.getMaxHealth() * percent;
            if (target.hurt(source, damage)) {
                ExecutionEffect.tryApply(target, ownerPlayer);

                Vec3 push = this.getDeltaMovement().normalize().scale(0.16D);
                target.push(push.x, 0.08D, push.z);
                target.hurtMarked = true;
            }

            level.sendParticles(ModParticles.VOID_FLAME.get(),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    14, 0.35D, 0.45D, 0.35D, 0.03D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    6, 0.3D, 0.4D, 0.3D, 0.01D);

            if (!this.playedHitSound) {
                this.playedHitSound = true;
                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.55F, 0.8F);
            }
        }
    }

    /**
     * 客户端拖尾：沿着横向剑刃的整条刃线洒出虚空火，形成一条连续的紫黑色尾迹。
     *
     * <p>横向剑气不再是一个面向镜头的圆贴片，所以粒子要按实体朝向铺在
     * “刀身”所在的横向线上，而不是随机铺一个方形。</p>
     */
    private void clientTrailParticles() {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double dirX = -Mth.sin(yaw);
        double dirZ = Mth.cos(yaw);
        double rightX = Mth.cos(yaw);
        double rightZ = Mth.sin(yaw);
        double width = this.getWaveWidth();
        double height = this.getWaveHeight();

        for (int i = 0; i < 4; i++) {
            double lateral = (this.random.nextDouble() - 0.5D) * width * 0.95D;
            double vertical = (this.random.nextDouble() - 0.5D) * height * 0.8D;
            double back = 0.10D + this.random.nextDouble() * 0.55D;
            this.level().addParticle(ModParticles.VOID_FLAME.get(),
                    this.getX() + rightX * lateral - dirX * back,
                    this.getY() + vertical,
                    this.getZ() + rightZ * lateral - dirZ * back,
                    (this.random.nextDouble() - 0.5D) * 0.02D,
                    0.012D + this.random.nextDouble() * 0.02D,
                    (this.random.nextDouble() - 0.5D) * 0.02D);
        }

        if (this.tickCount % 2 == 0) {
            double lateral = (this.random.nextDouble() - 0.5D) * width * 0.75D;
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL,
                    this.getX() + rightX * lateral - dirX * 0.5D,
                    this.getY() + (this.random.nextDouble() - 0.5D) * height * 0.35D,
                    this.getZ() + rightZ * lateral - dirZ * 0.5D,
                    0.0D, 0.01D, 0.0D);
        }
    }

    private void finish() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.VOID_FLAME.get(),
                    this.getX(), this.getY(), this.getZ(),
                    40, this.getWaveWidth() * 0.35D, this.getWaveHeight() * 0.45D, 0.30D, 0.04D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY(), this.getZ(),
                    24, this.getWaveWidth() * 0.35D, this.getWaveHeight() * 0.35D, 0.25D, 0.02D);
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.7F, 0.55F);
        }
        this.discard();
    }

    private static float yawOf(Vec3 direction) {
        return (float) (Mth.atan2(-direction.x, direction.z) * (180.0D / Math.PI));
    }

    private static float pitchOf(Vec3 direction) {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        return (float) (-Mth.atan2(direction.y, horizontal) * (180.0D / Math.PI));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Charge", this.getCharge());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setCharge(tag.getFloat("Charge"));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256.0D * 256.0D;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        // 剑气渲染是一大片月牙，比实体本身 0.5×0.5 的碰撞箱大得多；
        // 扩大剔除箱，避免剑气中心刚出屏就被整片裁掉。
        double width = this.getWaveWidth();
        double height = this.getWaveHeight();
        return this.getBoundingBox().inflate(width, height, width);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
