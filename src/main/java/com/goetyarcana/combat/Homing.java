package com.goetyarcana.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * 通用追踪（homing）工具：给虚空弩箭与裂魂聚晶射弹共用。
 *
 * <p>相比各自「找最近的目标」，这里多了三件事：</p>
 * <ol>
 *   <li>优先锁「施法者准星对准的那个目标」，玩家指哪打哪；</li>
 *   <li>视线（LOS）优先：被墙挡住的目标评分降低，近处才勉强接受；</li>
 *   <li>预测提前量：按飞行时间算目标会走到哪里，打移动目标不擦肩。</li>
 * </ol>
 */
public final class Homing {

    private Homing() {
    }

    /** 施法者当前准星指向的生物（没有则 null）。 */
    @Nullable
    public static LivingEntity crosshairTarget(Entity owner, double range) {
        if (!(owner instanceof LivingEntity living)) {
            return null;
        }
        Vec3 eye = living.getEyePosition(1.0F);
        Vec3 end = eye.add(living.getViewVector(1.0F).scale(range));
        BlockHitResult block = living.level().clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, living));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                living.level(), living, eye, stop,
                living.getBoundingBox().expandTowards(stop.subtract(eye)).inflate(1.0D),
                e -> e instanceof LivingEntity target && target != owner && target.isAlive()
                        && !target.isSpectator());
        return entityHit != null && entityHit.getEntity() instanceof LivingEntity livingHit
                ? livingHit : null;
    }

    /**
     * 选一个追踪目标。
     *
     * @param from    射弹当前位置
     * @param heading 当前飞行方向（单位向量）
     * @param range   搜索半径
     * @param minDot  允许的最小夹角余弦（0.15 ≈ 81° 锥角）
     */
    @Nullable
    public static LivingEntity bestTarget(Projectile projectile, Entity owner, Vec3 from, Vec3 heading,
                                          double range, double minDot,
                                          Predicate<LivingEntity> filter) {
        Vec3 lookVec = heading.lengthSqr() > 1.0E-6D ? heading.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        if (owner instanceof Player player) {
            LivingEntity aimed = crosshairTarget(player, range);
            if (aimed != null && filter.test(aimed)
                    && aimed.distanceToSqr(projectile) <= range * range) {
                // 关键：准星目标也必须在射弹「前方」；否则射弹会掉头去追身后的目标，
                // 看起来就像在追一个不存在的东西。
                Vec3 to = aimPoint(aimed).subtract(from);
                double distance = to.length();
                double dot = distance < 1.0E-4D ? 1.0D : to.scale(1.0D / distance).dot(lookVec);
                // 只给「近处、几乎正前方」的准星目标特权；否则交给几何评分，
                // 不然远处准星目标会把眼前的目标抢掉（射弹刚射出就掉头）。
                if ((distance <= 8.0D && dot >= 0.75D) || distance < 3.0D) {
                    return aimed;
                }
            }
        }
        List<LivingEntity> candidates = projectile.level().getEntitiesOfClass(LivingEntity.class,
                projectile.getBoundingBox().inflate(range), filter);
        Vec3 look = heading.lengthSqr() > 1.0E-6D ? heading.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        return candidates.stream()
                .filter(candidate -> {
                    Vec3 to = aimPoint(candidate).subtract(from);
                    double distance = to.length();
                    if (distance < 0.4D || distance > range) {
                        return false;
                    }
                    // 近处放宽锥角，远处必须基本朝向它
                    double cone = distance < 4.0D ? -0.2D : minDot;
                    double dot = to.scale(1.0D / distance).dot(look);
                    return dot >= cone;
                })
                .max(Comparator.comparingDouble(candidate -> score(projectile, from, look, candidate)))
                .orElse(null);
    }

    private static double score(Projectile projectile, Vec3 from, Vec3 look, LivingEntity candidate) {
        Vec3 to = aimPoint(candidate).subtract(from);
        double distance = to.length();
        if (distance < 0.4D) {
            return Double.NEGATIVE_INFINITY;
        }
        double dot = to.scale(1.0D / distance).dot(look);
        double score = dot * 1.3D - distance * 0.05D;
        if (distance < 3.5D) {
            score += 0.35D;
        }
        // 有视线权重；被挡住的远目标基本不考虑
        if (hasClearPath(projectile, candidate)) {
            score += 0.25D;
        } else if (distance > 8.0D) {
            score -= 0.8D;
        }
        return score;
    }

    /** 射弹到目标之间是否有方块阻挡。 */
    private static boolean hasClearPath(Entity from, Entity to) {
        Vec3 start = from.position().add(0.0D, from.getBbHeight() * 0.5D, 0.0D);
        Vec3 end = to.position().add(0.0D, to.getBbHeight() * 0.6D, 0.0D);
        BlockHitResult hit = from.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from));
        return hit.getType() == HitResult.Type.MISS
                || hit.getLocation().distanceToSqr(start) >= end.distanceToSqr(start) - 0.5D;
    }

    /** 瞄准点：目标身体中部偏上，比脚底更符合直觉。 */
    public static Vec3 aimPoint(LivingEntity target) {
        return new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ());
    }

    /** 预测提前量：按「距离 / 速度」估飞行时间，把目标速度乘上去。 */
    public static Vec3 leadPoint(LivingEntity target, Vec3 from, double speed, double leadFactor) {
        // 始终返回「相对 from 的方向向量」；返回世界坐标会被当成方向，射弹就会朝反方向飞
        Vec3 aim = aimPoint(target).subtract(from);
        double distance = aim.length();
        if (speed < 1.0E-4D || distance < 0.4D) {
            return aim;
        }
        double flightTicks = Math.min(16.0D, distance / speed);
        return aim.add(target.getDeltaMovement().scale(flightTicks * leadFactor));
    }

    /** 朝目标转向：保留原速度，只改方向。closer = 更贴近目标时转得更紧。 */
    public static Vec3 turn(Vec3 motion, Vec3 desired, double baseTurn, double distance) {
        double speed = Math.max(0.2D, motion.length());
        Vec3 dir = motion.lengthSqr() > 1.0E-6D ? motion.normalize() : desired.normalize();
        // 距离近时把转向系数抬到 baseTurn * 1.6，避免贴脸绕圈
        double turn = net.minecraft.util.Mth.clamp(
                baseTurn * (1.0D + Math.max(0.0D, 6.0D - distance) * 0.1D), 0.0D, 0.95D);
        Vec3 steered = dir.lerp(desired.normalize(), turn);
        if (steered.lengthSqr() < 1.0E-6D) {
            steered = desired;
        }
        return steered.normalize().scale(speed);
    }
}
