package com.goetyarcana.entity;

import com.goetyarcana.VoidBlastBalance;
import com.goetyarcana.registry.ModEntities;
import com.goetyarcana.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 自然生成的虚空苦力怕（末地，中立）。
 *
 * <p>平时不主动攻击。你盯着它的眼睛看、它也会盯着你；连续对视 10 秒它就会彻底愤怒，
 * 瞬移到你面前、引信 1 秒后自爆（这段有 50% 全类型减伤，就算被打死也会炸）。</p>
 *
 * <p>只在末地外岛生成，且必须已经击杀过末影龙；一次只会存在一只。</p>
 */
public class VoidCreeper extends Monster {

    private static final EntityDataAccessor<Integer> DATA_ALERT =
            SynchedEntityData.defineId(VoidCreeper.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ANGRY =
            SynchedEntityData.defineId(VoidCreeper.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_FUSE =
            SynchedEntityData.defineId(VoidCreeper.class, EntityDataSerializers.INT);

    private int stareTicks;
    private int fuseTicks = VoidBlastBalance.VOID_CREEPER_FUSE_TICKS;
    private boolean exploded;
    private boolean lootDropped;

    public VoidCreeper(EntityType<? extends VoidCreeper> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VoidBlastBalance.VOID_CREEPER_HEALTH)
                .add(Attributes.ARMOR, VoidBlastBalance.VOID_CREEPER_ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, VoidBlastBalance.VOID_CREEPER_TOUGHNESS)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ALERT, 0);
        this.entityData.define(DATA_ANGRY, false);
        this.entityData.define(DATA_FUSE, 0);
    }

    public int getAlert() {
        return this.entityData.get(DATA_ALERT);
    }

    public boolean isAngry() {
        return this.entityData.get(DATA_ANGRY);
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE);
    }

    /** 只有玩家盯着它看的时候才会逼近。 */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);

        if (this.isAngry()) {
            this.angryTick();
        } else {
            this.neutralTick();
        }
    }

    private void neutralTick() {
        Player watcher = this.findWatcher();
        if (watcher != null) {
            this.getLookControl().setLookAt(watcher, 40.0F, 40.0F);
            this.stareTicks = Math.min(VoidBlastBalance.VOID_CREEPER_STARE_TICKS,
                    this.stareTicks + 1);
            if (this.level() instanceof ServerLevel server && this.stareTicks % 10 == 0) {
                server.sendParticles(ParticleTypes.WITCH,
                        this.getX(), this.getY() + this.getBbHeight() * 0.8D, this.getZ(),
                        2, 0.25D, 0.25D, 0.25D, 0.0D);
            }
            if (this.stareTicks % 40 == 0) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.CREEPER_PRIMED,
                        SoundSource.HOSTILE, 0.35F, 1.6F);
            }
            // 警告动画：对视过半、接近满的时间点各提示一次（血条上方的动作栏文字 + 音效）
            if ((this.stareTicks == 100 || this.stareTicks == 160) && watcher != null) {
                watcher.displayClientMessage(net.minecraft.network.chat.Component
                        .translatable("message.goetyarcana.void_creeper.stare"), true);
                this.level().playSound(null, watcher.blockPosition(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0F,
                        1.0F + this.stareTicks / 240.0F);
            }
            if (this.stareTicks >= VoidBlastBalance.VOID_CREEPER_STARE_TICKS) {
                this.becomeAngry(watcher);
            }
        } else {
            this.stareTicks = Math.max(0, this.stareTicks - 2);
        }
        this.entityData.set(DATA_ALERT, this.stareTicks);
    }

    private void becomeAngry(@Nullable Player watcher) {
        this.entityData.set(DATA_ANGRY, true);
        this.entityData.set(DATA_ALERT, VoidBlastBalance.VOID_CREEPER_STARE_TICKS);
        this.level().playSound(null, this.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.HOSTILE, 1.2F, 0.7F);
        if (watcher != null && this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 40, 0.6D, 1.0D, 0.6D, 0.15D);
        }
    }

    private void angryTick() {
        Player target = this.level().getNearestPlayer(this, 32.0D);
        if (target == null || target.distanceToSqr(this) > 24.0D * 24.0D) {
            // 玩家跑远了就像原版苦力怕一样不再引爆
            this.discard();
            return;
        }
        if (this.fuseTicks == VoidBlastBalance.VOID_CREEPER_FUSE_TICKS && !this.hasCharged()) {
            this.charged = true;
            Vec3 spawn = target.position();
            this.teleportTo(spawn.x, spawn.y, spawn.z);
            this.level().playSound(null, this.blockPosition(), SoundEvents.CREEPER_PRIMED,
                    SoundSource.HOSTILE, 1.0F, 0.8F);
        }
        this.entityData.set(DATA_FUSE, Math.max(0, this.fuseTicks));
        if (this.level() instanceof ServerLevel server) {
            float progress = 1.0F - Math.max(0, this.fuseTicks)
                    / (float) VoidBlastBalance.VOID_CREEPER_FUSE_TICKS;
            server.sendParticles(com.goetyarcana.registry.ModParticles.VOID_FLAME.get(),
                    this.getX(), this.getY() + 0.8D, this.getZ(), 1 + (int) (progress * 4),
                    0.3D, 0.4D, 0.3D, 0.02D);
        }
        if (--this.fuseTicks <= 0) {
            this.explode();
        }
    }

    private boolean charged;
    private boolean hasCharged() {
        return this.charged;
    }

    /** 头部的碰撞盒：只有视线真的落在头上才算对视（看身体不算）。 */
    private AABB headBox() {
        double half = 0.25D;
        double y = this.getY() + this.getBbHeight() * 0.62D;
        return new AABB(this.getX() - half, y, this.getZ() - half,
                this.getX() + half, y + this.getBbHeight() * 0.3D, this.getZ() + half);
    }

    /** 玩家是否正盯着它的头（双方都要看向对方）。 */
    @Nullable
    private Player findWatcher() {
        List<Player> players = this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(20.0D),
                player -> player.isAlive() && !player.isSpectator());
        AABB head = this.headBox();
        for (Player player : players) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getViewVector(1.0F);
            // 视线射线与头部小盒的交点
            var hit = head.clip(eye, eye.add(look.scale(24.0D)));
            if (hit.isEmpty()) {
                continue;
            }
            // 中间不能有方块挡着
            var blockHit = this.level().clip(new net.minecraft.world.level.ClipContext(
                    eye, hit.get(), net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, player));
            if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                continue;
            }
            Vec3 toPlayer = eye.subtract(this.getEyePosition()).normalize();
            if (this.getViewVector(1.0F).dot(toPlayer) < 0.5D && this.stareTicks > 20) {
                continue;
            }
            return player;
        }
        return null;
    }

    // ---------------------------------------------------------------- 爆炸

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.isAngry() && source.getEntity() instanceof Player attacker) {
            // 被玩家打了也会立刻进入愤怒
            this.stareTicks = VoidBlastBalance.VOID_CREEPER_STARE_TICKS;
            this.becomeAngry(attacker);
        }
        if (this.isAngry() && !this.exploded) {
            // 瞬移 + 引信这段 50% 全类型减伤
            amount *= 1.0F - VoidBlastBalance.VOID_CREEPER_CHARGE_RESIST;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && !this.exploded) {
            this.explode();
        }
        super.die(source);
    }

    private void explode() {
        if (!(this.level() instanceof ServerLevel server) || this.exploded) {
            return;
        }
        this.exploded = true;
        // 自爆不是「被打死」，死亡掉落流程不会跑，这里手动补一次
        LivingEntity killer = this.getLastHurtByMob();
        int looting = killer instanceof Player player
                ? net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                        net.minecraft.world.item.enchantment.Enchantments.MOB_LOOTING,
                        player.getMainHandItem())
                : 0;
        this.dropVoidShard(looting);
        float power = switch (server.getDifficulty()) {
            case PEACEFUL, EASY -> VoidBlastBalance.VOID_CREEPER_DAMAGE_EASY;
            case NORMAL -> VoidBlastBalance.VOID_CREEPER_DAMAGE_NORMAL;
            default -> VoidBlastBalance.VOID_CREEPER_DAMAGE_HARD;
        };
        double radius = VoidBlastBalance.EXPLOSION_RADIUS;
        AABB box = this.getBoundingBox().inflate(radius);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != this && entity.isAlive())) {
            double distance = Math.sqrt(victim.getBoundingBox().distanceToSqr(
                    this.position().add(0.0D, 0.5D, 0.0D)));
            if (distance > radius) {
                continue;
            }
            double factor = 1.0D - (distance / radius) * (1.0D - VoidBlastBalance.EXPLOSION_MIN_FACTOR);
            victim.hurt(this.damageSources().explosion(this, this), (float) (power * factor));
        }
        // 自定义爆炸表现：冲击波环 + 虚空火 + 反向传送门，不用原版的爆炸云
        com.goetyarcana.combat.VoidBlastVisuals.explode(server,
                this.getX(), this.getY() + 0.4D, this.getZ());
        server.sendParticles(com.goetyarcana.registry.ModParticles.VOID_FLAME.get(),
                this.getX(), this.getY() + 0.5D, this.getZ(), 40, 1.0D, 0.8D, 1.0D, 0.06D);
        server.sendParticles(ParticleTypes.REVERSE_PORTAL,
                this.getX(), this.getY() + 0.5D, this.getZ(), 50, 1.0D, 0.8D, 1.0D, 0.12D);
        server.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 1.2F, 0.9F);
        this.discard();
    }

    // ---------------------------------------------------------------- 掉落

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.dropVoidShard(looting);
    }

    /** 虚空碎片掉落；每只只会掉一次（自爆与被打死都走这里）。 */
    private void dropVoidShard(int looting) {
        if (this.lootDropped) {
            return;
        }
        this.lootDropped = true;
        float chance = VoidBlastBalance.VOID_SHARD_CHANCE
                + looting * VoidBlastBalance.VOID_SHARD_LOOTING_BONUS;
        if (this.random.nextFloat() < chance) {
            ItemEntity drop = new ItemEntity(this.level(),
                    this.getX(), this.getY() + 0.4D, this.getZ(),
                    new ItemStack(ModItems.VOID_SHARD.get()));
            // 掉落物不被自己的爆炸炸掉
            drop.setInvulnerable(true);
            drop.setPickUpDelay(10);
            this.level().addFreshEntity(drop);
        }
    }

    // ---------------------------------------------------------------- 生成

    /** 是否在末地城 / Goety 的终末永狱结构范围内。 */
    public static boolean nearEndStructure(ServerLevel server, BlockPos pos) {
        var manager = server.structureManager();
        var structures = manager.getAllStructuresAt(pos);
        if (structures.isEmpty()) {
            return false;
        }
        var registry = server.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        for (var structure : structures.keySet()) {
            var key = registry.getKey(structure);
            if (key == null) {
                continue;
            }
            if (key.equals(new net.minecraft.resources.ResourceLocation("minecraft", "end_city"))
                    || key.equals(new net.minecraft.resources.ResourceLocation("goety", "final_terminal"))) {
                return true;
            }
        }
        return false;
    }

    /** 末地外岛 + 已击杀末影龙 + 附近没有同类。 */
    public static boolean checkSpawnRules(EntityType<VoidCreeper> type, ServerLevelAccessor level,
                                          MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        // 主岛（末影龙所在）半径约 1000 格内不生成
        if (pos.getX() * pos.getX() + pos.getZ() * pos.getZ() < 900 * 900) {
            return false;
        }
        ServerLevel server = level.getLevel();
        var fight = server.getDragonFight();
        if (fight == null || !fight.hasPreviouslyKilledDragon()) {
            return false;
        }
        // 结构附近（末地城 / 终末永狱）允许聚得密一些
        double radius = nearEndStructure(server, pos) ? 64.0D : 128.0D;
        return server.getEntitiesOfClass(VoidCreeper.class,
                new AABB(pos).inflate(radius)).isEmpty();
    }
}
