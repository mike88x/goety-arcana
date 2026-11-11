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
        if (movement.lengthSqr() < 1.0E-6D) {
            // 极端情况下（被其它模组改了速度），按朝向继续飞，避免剑气原地不动。
            movement = Vec3.directionFromRotation(this.getXRot(), this.getYRot())
                    .scale(VoidScytheBalance.WAVE_SPEED);
        }

        Vec3 from = this.position();
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
        this.setDeltaMovement(movement);
        Vec3 to = this.position();

        // 让实体朝向与运动方向一致：客户端渲染和后续旋转都靠它。
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);

        this.damageTargets(from, to);
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
     * 客户端拖尾：剑气所在平面不断洒出虚空火，形成一条连续的紫黑色尾迹。
     */
    private void clientTrailParticles() {
        double width = this.getWaveWidth();
        double height = this.getWaveHeight();
        int count = 3;
        for (int i = 0; i < count; i++) {
            double offsetX = (this.random.nextDouble() - 0.5D) * width;
            double offsetY = (this.random.nextDouble() - 0.5D) * height;
            double offsetZ = (this.random.nextDouble() - 0.5D) * 0.35D;
            this.level().addParticle(ModParticles.VOID_FLAME.get(),
                    this.getX() + offsetX,
                    this.getY() + offsetY,
                    this.getZ() + offsetZ,
                    (this.random.nextDouble() - 0.5D) * 0.015D,
                    0.012D + this.random.nextDouble() * 0.02D,
                    (this.random.nextDouble() - 0.5D) * 0.015D);
        }

        if (this.tickCount % 3 == 0) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL,
                    this.getX() + (this.random.nextDouble() - 0.5D) * width,
                    this.getY() + (this.random.nextDouble() - 0.5D) * height,
                    this.getZ(),
                    0.0D, 0.01D, 0.0D);
        }
    }

    private void finish() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.VOID_FLAME.get(),
                    this.getX(), this.getY(), this.getZ(),
                    36, this.getWaveWidth() * 0.45D, this.getWaveHeight() * 0.45D, 0.35D, 0.04D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY(), this.getZ(),
                    24, this.getWaveWidth() * 0.4D, this.getWaveHeight() * 0.4D, 0.3D, 0.02D);
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
