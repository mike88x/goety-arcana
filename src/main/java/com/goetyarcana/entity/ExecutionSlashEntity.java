package com.goetyarcana.entity;

import com.goetyarcana.registry.ModEntities;
import com.goetyarcana.registry.ModParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 处决结算的专属动画载体。
 *
 * <p>debuff 结束后先在目标身上生成这个不可见的“处决标记”，由
 * {@link com.goetyarcana.effect.ExecutionEffect} 延迟 12 tick 再真正执行
 * kill()/hurt()；这段时间客户端渲染一把巨大的虚空镰刀斩下，并在目标周围
 * 爆出虚空火。</p>
 */
public class ExecutionSlashEntity extends Entity {

    public static final int LIFETIME = 30;

    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(ExecutionSlashEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID targetUUID;

    public ExecutionSlashEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ExecutionSlashEntity(Level level, LivingEntity target) {
        this(ModEntities.EXECUTION_SLASH.get(), level);
        this.targetUUID = target.getUUID();
        this.moveToTarget(target);
        float size = Math.max(target.getBbWidth(), target.getBbHeight());
        this.setScale(Mth.clamp(size * 1.35F, 1.6F, 4.2F));
        this.setYRot(target.getYRot());
        this.yRotO = this.getYRot();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SCALE, 2.0F);
    }

    public float getScale() {
        return this.entityData.get(DATA_SCALE);
    }

    public void setScale(float scale) {
        this.entityData.set(DATA_SCALE, Mth.clamp(scale, 1.0F, 6.0F));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            this.clientParticles();
            return;
        }

        if (this.tickCount > LIFETIME) {
            this.discard();
            return;
        }

        LivingEntity target = this.getTargetEntity();
        if (target != null && target.isAlive()) {
            this.moveToTarget(target);
            this.setYRot(target.getYRot());
        }
    }

    private void moveToTarget(LivingEntity target) {
        this.setPos(target.getX(),
                target.getY() + target.getBbHeight() * 0.55D,
                target.getZ());
    }

    @Nullable
    private LivingEntity getTargetEntity() {
        if (this.targetUUID == null || !(this.level() instanceof ServerLevel server)) {
            return null;
        }
        Entity entity = server.getEntity(this.targetUUID);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void clientParticles() {
        double radius = this.getScale() * 0.45D;
        for (int i = 0; i < 3; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double r = radius * (0.4D + this.random.nextDouble() * 0.9D);
            this.level().addParticle(ModParticles.VOID_FLAME.get(),
                    this.getX() + Math.cos(angle) * r,
                    this.getY() + (this.random.nextDouble() - 0.5D) * radius,
                    this.getZ() + Math.sin(angle) * r,
                    0.0D, 0.015D + this.random.nextDouble() * 0.02D, 0.0D);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Scale", this.getScale());
        if (this.targetUUID != null) {
            tag.putUUID("Target", this.targetUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setScale(tag.getFloat("Scale"));
        if (tag.hasUUID("Target")) {
            this.targetUUID = tag.getUUID("Target");
        }
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
        return distance < 128.0D * 128.0D;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(this.getScale() * 2.0D);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
