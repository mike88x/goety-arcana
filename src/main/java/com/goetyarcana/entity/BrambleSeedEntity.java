package com.goetyarcana.entity;

import com.Polarice3.Goety.api.entities.IOwned;
import com.goetyarcana.BrambleBindBalance;
import com.goetyarcana.registry.ModEntities;
import com.goetyarcana.registry.ModParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * 荆棘藤缚射出的种子。
 *
 * <p>飞行 → 插在地上（{@code stuck}，之后完全不动）→ 在
 * {@link BrambleBindBalance#SEED_LIFETIME} tick 内持续检测周围范围，一有敌对生物就长出
 * {@link BrambleVinesEntity} 缠住（最多 8 个），然后种子消失；10 秒没等到目标就自己消失。</p>
 *
 * <p>范围与缠绕时长由施法时的魔杖/长袍决定（3×3 / 7×7 / 9×9，5 秒 / 15 秒），
 * 长袍还会让藤蔓附带藤毒。</p>
 */
public class BrambleSeedEntity extends ThrowableProjectile {

    private static final String TAG_STUCK = "Stuck";
    private static final String TAG_AGE = "Age";
    private static final String TAG_RADIUS = "Radius";
    private static final String TAG_TICKS = "EntangleTicks";
    private static final String TAG_POISON = "Poison";
    private static final String TAG_MAX = "MaxTargets";

    /** 同步给客户端，落地后不要再自己飞。 */
    private static final EntityDataAccessor<Boolean> DATA_STUCK =
            SynchedEntityData.defineId(BrambleSeedEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean stuck;
    private int age;
    private double checkRadius = BrambleBindBalance.CHECK_RADIUS;
    private int entangleTicks = BrambleBindBalance.ENTANGLE_TICKS;
    private boolean poison;
    private int maxTargets = BrambleBindBalance.MAX_TARGETS;
    /** 这颗种子已经缠过的目标 id，避免同一只怪被反复缠。 */
    private final java.util.Set<Integer> caughtIds = new java.util.HashSet<>();

    public BrambleSeedEntity(EntityType<? extends BrambleSeedEntity> type, Level level) {
        super(type, level);
    }

    public BrambleSeedEntity(Level level, LivingEntity owner) {
        super(ModEntities.BRAMBLE_SEED.get(), owner, level);
    }

    /** 由法术按魔杖/长袍配置范围、缠绕时长与藤毒。 */
    public void configure(double radius, int ticks, boolean withPoison, int maxTargets) {
        this.checkRadius = radius;
        this.entangleTicks = ticks;
        this.poison = withPoison;
        this.maxTargets = Math.max(1, maxTargets);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_STUCK, false);
    }

    public boolean isStuck() {
        return this.stuck || this.entityData.get(DATA_STUCK);
    }

    @Override
    public void tick() {
        if (this.isStuck()) {
            // 插在地里：完全不动
            this.setDeltaMovement(Vec3.ZERO);
            if (this.level().isClientSide) {
                return;
            }
            if (++this.age > BrambleBindBalance.SEED_LIFETIME) {
                this.discard();
                return;
            }
            if (this.age % 5 == 0) {
                this.tryEntangle();
            }
            if (this.age % 10 == 0 && this.level() instanceof ServerLevel server) {
                this.showRange(server);
            }
            return;
        }
        super.tick();
        if (!this.level().isClientSide && this.onGround()) {
            this.stick();
        }
        // 飞行时拖一串荒野符文，让玩家看得见种子
        if (this.level() instanceof ServerLevel server && this.tickCount % 2 == 0) {
            server.sendParticles(ModParticles.WILD_RUNE.get(),
                    this.getX(), this.getY(), this.getZ(), 2, 0.08D, 0.08D, 0.08D, 0.02D);
        }
        if (!this.level().isClientSide && this.tickCount > BrambleBindBalance.SEED_LIFETIME * 2) {
            this.discard();
        }
    }

    /** 插地待机时把判定范围用荒野符文勾出来：越靠边缘越密，形成方形渐变。 */
    private void showRange(ServerLevel server) {
        double radius = this.checkRadius;
        int steps = radius > 3.0D ? 13 : 7;
        double step = radius * 2.0D / (steps - 1);
        for (int ix = 0; ix < steps; ix++) {
            for (int iz = 0; iz < steps; iz++) {
                double x = this.getX() - radius + ix * step;
                double z = this.getZ() - radius + iz * step;
                double edge = Math.max(Math.abs(x - this.getX()), Math.abs(z - this.getZ())) / radius;
                if (this.random.nextFloat() > edge * edge) {
                    continue;
                }
                server.sendParticles(ModParticles.WILD_RUNE.get(),
                        x, this.getY() + 0.05D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide) {
            this.stickAt(result.getLocation());
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // 种子不撞生物，只认地面。
    }

    private void stick() {
        this.stickAt(this.position());
    }

    /** 插在地上：压低半埋，之后固定不动。 */
    private void stickAt(Vec3 where) {
        if (this.isStuck()) {
            return;
        }
        this.stuck = true;
        this.entityData.set(DATA_STUCK, true);
        this.age = 0;
        this.setPos(where.x, where.y + 0.05D, where.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        if (!this.level().isClientSide) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GRASS_PLACE, SoundSource.PLAYERS, 1.0F, 1.25F);
            this.tryEntangle();
        }
    }

    /** 找范围内的敌对生物，有就长藤蔓。 */
    private void tryEntangle() {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (this.caughtIds.size() >= this.maxTargets) {
            return;
        }
        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(this.checkRadius, 2.0D, this.checkRadius),
                this::isHostileTarget);
        targets.removeIf(target -> this.caughtIds.contains(target.getId()));
        if (targets.isEmpty()) {
            return;
        }
        targets.sort(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
        boolean caughtAny = false;
        for (LivingEntity target : targets) {
            if (this.caughtIds.size() >= this.maxTargets) {
                break;
            }
            if (this.caughtIds.add(target.getId())) {
                server.addFreshEntity(new BrambleVinesEntity(server, target, this.entangleTicks, this.poison));
                caughtAny = true;
            }
        }
        if (!caughtAny) {
            return;
        }
        // 陷阱在本轮结束前一直有效：这里不再 discard，后续进入范围的敌人也会被缠住
        server.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 1.2F, 0.7F);
        server.sendParticles(ModParticles.WILD_RUNE.get(),
                this.getX(), this.getY() + 0.2D, this.getZ(), 24, 1.0D, 0.3D, 1.0D, 0.02D);
    }

    private boolean isHostileTarget(LivingEntity entity) {
        if (!entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        // 有些 BOSS 只继承 Monster / 只挂 MONSTER 分类，并不实现 Enemy，这里放宽判定
        boolean hostile = entity instanceof Enemy
                || entity instanceof net.minecraft.world.entity.monster.Monster
                || entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER;
        if (!hostile) {
            return false;
        }
        if (entity == this.getOwner()) {
            return false;
        }
        // 施法者自己的 Goety 仆从不算敌人
        return !(entity instanceof IOwned owned && owned.getTrueOwner() == this.getOwner());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_STUCK, this.stuck);
        tag.putInt(TAG_AGE, this.age);
        tag.putDouble(TAG_RADIUS, this.checkRadius);
        tag.putInt(TAG_TICKS, this.entangleTicks);
        tag.putBoolean(TAG_POISON, this.poison);
        tag.putInt(TAG_MAX, this.maxTargets);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.stuck = tag.getBoolean(TAG_STUCK);
        this.age = tag.getInt(TAG_AGE);
        if (tag.contains(TAG_RADIUS)) {
            this.checkRadius = tag.getDouble(TAG_RADIUS);
        }
        if (tag.contains(TAG_TICKS)) {
            this.entangleTicks = tag.getInt(TAG_TICKS);
        }
        this.poison = tag.getBoolean(TAG_POISON);
        if (tag.contains(TAG_MAX)) {
            this.maxTargets = tag.getInt(TAG_MAX);
        }
        this.entityData.set(DATA_STUCK, this.stuck);
        if (this.stuck) {
            this.setNoGravity(true);
        }
    }
}
