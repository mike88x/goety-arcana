package com.goetyarcana.entity;

import com.goetyarcana.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 纯客户端表现的虚空爆炸动画：先收缩，再扩散双环。
 */
public class VoidExplosionEntity extends Entity {

    public static final int MAX_AGE = 24;
    private int age;

    public VoidExplosionEntity(EntityType<? extends VoidExplosionEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public VoidExplosionEntity(Level level, Vec3 pos) {
        this(ModEntities.VOID_EXPLOSION.get(), level);
        this.setPos(pos.x, pos.y, pos.z);
    }

    @Override
    public void tick() {
        super.tick();
        if (++this.age > MAX_AGE) {
            this.discard();
        }
    }

    public int getAge() {
        return this.age;
    }

    public float getProgress() {
        return Math.min(1.0F, this.age / (float) MAX_AGE);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
