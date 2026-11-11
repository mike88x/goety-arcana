package com.goetyarcana.entity;

import com.Polarice3.Goety.common.effects.GoetyEffects;
import com.Polarice3.Goety.common.entities.projectiles.SpellHurtingProjectile;
import com.Polarice3.Goety.utils.MobUtil;
import com.goetyarcana.SoulBurstBalance;
import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.registry.ModEntities;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;

/**
 * 灵魂弹：聚晶射出的虚空方块。
 *
 * 继承 Goety 的 {@link SpellHurtingProjectile}，复用官方的命中检测、
 * 阵营判定（不误伤施法者与仆从）等逻辑；飞行是匀速直线。
 */
public class SoulBurstEntity extends SpellHurtingProjectile {

    private static final String TAG_VOID_CHARGED = "VoidCharged";

    /**
     * 自定义伤害类型 goetyarcana:soul_burst。
     *
     * 它在 data/minecraft/tags/damage_type/bypasses_invulnerability.json 里被登记，
     * 而 Goety 的 MobUtil.bossHurt 逻辑是：
     *     if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) damage = min(damage, cap);
     * 所以带上这个标签后，灵魂弹的伤害（含百分比部分）不会被 Boss 伤害上限削减。
     */
    private static final ResourceKey<DamageType> SOUL_BURST_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "soul_burst"));

    /** 虚空之蚀持续时间：5 秒 */
    private static final int VOID_TOUCHED_TICKS = 100;

    /** 是否由虚空系法杖（虚空魔杖）施放 */
    private boolean voidCharged;

    /** 是否已经炸过，避免一次射弹结算两次范围伤害 */
    private boolean exploded;

    public SoulBurstEntity(EntityType<? extends SoulBurstEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    /**
     * @param spawnPos  出生点（一般取 玩家眼睛 + 视线方向偏移，即准星位置）
     * @param direction 飞行方向（单位向量）
     */
    public SoulBurstEntity(Level level, LivingEntity owner, Vec3 spawnPos, Vec3 direction) {
        super(ModEntities.SOUL_BURST.get(), owner, direction.x, direction.y, direction.z, level);
        this.setNoGravity(true);
        this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        // 关键：一开始就给定满速，而不是像原版那样从 0 慢慢加速
        Vec3 velocity = direction.normalize();
        this.setDeltaMovement(velocity.scale(SoulBurstBalance.PROJECTILE_SPEED));
        // 朝向 = 飞行方向：客户端渲染方块和拖尾时要用它还原方向
        this.setYRot((float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)));
        this.setXRot((float) Math.toDegrees(Math.atan2(-velocity.y,
                Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z))));
    }

    public boolean isVoidCharged() {
        return this.voidCharged;
    }

    public void setVoidCharged(boolean voidCharged) {
        this.voidCharged = voidCharged;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_VOID_CHARGED, this.voidCharged);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.voidCharged = tag.getBoolean(TAG_VOID_CHARGED);
    }

    /**
     * 匀速直线飞行（父类是按「加速度 + 阻尼」推进的，这里直接覆盖掉）。
     */
    @Override
    public void travel() {
        Vec3 velocity = this.getDeltaMovement();
        this.setPos(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > SoulBurstBalance.PROJECTILE_LIFETIME) {
            // 虚空魔杖：飞到寿命尽头、什么都没碰到也会炸开
            if (this.voidCharged && !this.exploded && this.level() instanceof ServerLevel serverLevel) {
                this.groundExplosion(serverLevel);
            }
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }

        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        if (isProtected(target)) {
            return;
        }

        float damage = SoulBurstBalance.FIXED_DAMAGE + this.getExtraDamage();
        if (target instanceof LivingEntity livingTarget) {
            damage += livingTarget.getMaxHealth() * SoulBurstBalance.PERCENT_OF_MAX_HEALTH;
        }

        DamageSource source = this.soulBurstDamage(owner);
        target.hurt(source, damage);

        if (this.voidCharged && target instanceof LivingEntity livingTarget) {
            // 虚空魔杖专属 1：虚空之蚀 + 夺取正面效果
            this.applyVoidErosion(livingTarget, owner);
            // 虚空魔杖专属 2：范围伤害 + 特效
            this.voidExplosion(livingTarget, owner, source, damage);
        }
    }

    private DamageSource soulBurstDamage(Entity owner) {
        Holder<DamageType> holder = this.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(SOUL_BURST_DAMAGE);
        return new DamageSource(holder, this, owner);
    }

    /**
     * 由于伤害类型带 bypasses_invulnerability 标签，这里必须自己排除创造模式玩家、
     * 观察者和处于无敌状态的实体，避免误伤。
     */
    private static boolean isProtected(Entity target) {
        if (target.isSpectator() || target.isInvulnerable()) {
            return true;
        }
        return target instanceof Player player && player.isCreative();
    }

    /**
     * 给目标挂 5 秒「虚空之蚀」，并把目标身上所有正面效果转移给施法者。
     */
    private void applyVoidErosion(LivingEntity target, Entity owner) {
        target.addEffect(new MobEffectInstance(GoetyEffects.VOID_TOUCHED.get(), VOID_TOUCHED_TICKS, 0));

        if (!(owner instanceof LivingEntity livingOwner)) {
            return;
        }

        List<MobEffectInstance> stealable = new ArrayList<>();
        for (MobEffectInstance instance : target.getActiveEffects()) {
            if (instance.getEffect().isBeneficial()) {
                stealable.add(instance);
            }
        }

        for (MobEffectInstance instance : stealable) {
            target.removeEffect(instance.getEffect());
            livingOwner.addEffect(new MobEffectInstance(instance));
        }

        if (!stealable.isEmpty() && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    livingOwner.getX(), livingOwner.getY() + 1.0D, livingOwner.getZ(),
                    12, 0.4D, 0.6D, 0.4D, 0.02D);
        }
    }

    /**
     * 直接命中敌人时的范围伤害：命中点周围的其他敌对目标吃一半伤害，不误伤施法者的仆从。
     */
    private void voidExplosion(LivingEntity primary, Entity owner, DamageSource source, float damage) {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.exploded) {
            return;
        }
        this.exploded = true;
        float aoeDamage = damage * SoulBurstBalance.VOID_AOE_MULTIPLIER;
        for (LivingEntity other : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SoulBurstBalance.VOID_AOE_RADIUS))) {
            if (other == primary || other == owner || isProtected(other)) {
                continue;
            }
            if (owner != null && MobUtil.areAllies(owner, other)) {
                continue;
            }
            other.hurt(source, aoeDamage);
        }

        this.voidBurstEffect(serverLevel);
    }

    /**
     * 没打中敌人（撞方块 / 飞到寿命尽头）时的范围伤害：
     * 每个范围内目标单独按「固定 + 自身最大生命值百分比」的一半结算。
     */
    private void groundExplosion(ServerLevel level) {
        if (this.exploded) {
            return;
        }
        this.exploded = true;

        Entity owner = this.getOwner();
        DamageSource source = this.soulBurstDamage(owner);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SoulBurstBalance.VOID_AOE_RADIUS))) {
            if (other == owner || isProtected(other)) {
                continue;
            }
            if (owner != null && MobUtil.areAllies(owner, other)) {
                continue;
            }
            float damage = SoulBurstBalance.FIXED_DAMAGE + this.getExtraDamage()
                    + other.getMaxHealth() * SoulBurstBalance.PERCENT_OF_MAX_HEALTH;
            other.hurt(source, damage * SoulBurstBalance.VOID_AOE_MULTIPLIER);
        }

        this.voidBurstEffect(level);
    }

    /**
     * 范围伤害特效：两圈向外扩散的虚空粒子环 + 中心闪光 + 低频呼啸。
     */
    private void voidBurstEffect(ServerLevel level) {
        double radius = SoulBurstBalance.VOID_AOE_RADIUS;
        for (int ring = 0; ring < 2; ring++) {
            double r = radius * (ring == 0 ? 0.5D : 1.0D);
            int points = ring == 0 ? 12 : 16;
            ParticleOptions type = ring == 0 ? ParticleTypes.WITCH : ParticleTypes.REVERSE_PORTAL;
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0D / points) * i;
                double px = this.getX() + Math.cos(angle) * r;
                double pz = this.getZ() + Math.sin(angle) * r;
                level.sendParticles(type, px, this.getY() + 0.2D, pz, 1, 0.0D, 0.08D, 0.0D, 0.01D);
            }
        }
        level.sendParticles(ParticleTypes.FLASH,
                this.getX(), this.getY() + 0.4D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.7F, 0.6F);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            // 虚空魔杖：撞到方块（没打中敌人）也会炸开
            if (this.voidCharged && !(result instanceof EntityHitResult)) {
                this.groundExplosion(serverLevel);
            }
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    this.getX(), this.getY() + 0.2D, this.getZ(),
                    16, 0.3D, 0.3D, 0.3D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY() + 0.2D, this.getZ(),
                    8, 0.2D, 0.2D, 0.2D, 0.02D);
        }
        this.discard();
    }

    /**
     * 拖尾：沿飞行方向补几个紫色虚空粒子，看起来是一条连续的光带而不是一个个点。
     */
    @Override
    public void trailParticle() {
        if (!this.level().isClientSide) {
            return;
        }
        Vec3 velocity = this.getDeltaMovement();
        for (int i = 0; i < 4; i++) {
            double back = i / 4.0D;
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL,
                    this.getX() - velocity.x * back,
                    this.getY() + 0.1D - velocity.y * back,
                    this.getZ() - velocity.z * back,
                    0.0D, 0.0D, 0.0D);
        }
        this.level().addParticle(ParticleTypes.WITCH,
                this.getX(), this.getY() + 0.1D, this.getZ(),
                0.0D, 0.0D, 0.0D);
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.REVERSE_PORTAL;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
