package com.goetyarcana.combat;

import com.goetyarcana.VoidScytheBalance;
import com.goetyarcana.registry.ModTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;

/**
 * 怪物分级：普通 / 精英 / Boss。
 *
 * <p>判定顺序：</p>
 * <ol>
 *   <li>实体类型标签 {@code goetyarcana:bosses} / {@code goetyarcana:elites}；</li>
 *   <li>原版 Boss（末影龙、凋灵）；</li>
 *   <li>Goety 的 Boss 包（{@code com.Polarice3.Goety.common.entities.boss}）；</li>
 *   <li>最大生命值兜底：≥200 视为 Boss，≥60 视为精英。</li>
 * </ol>
 *
 * <p>整合包作者给实体加上 {@code goetyarcana:elite} 的 scoreboard tag 或
 * 在实体 persistent data 里写 {@code goetyarcana:elite=true}，也会被视作精英。</p>
 */
public final class CombatTier {

    public enum Tier {
        NORMAL,
        ELITE,
        BOSS
    }

    private static final String GOETY_BOSS_PACKAGE =
            "com.Polarice3.Goety.common.entities.boss.";

    public static Tier of(LivingEntity entity) {
        if (isBoss(entity)) {
            return Tier.BOSS;
        }
        if (isElite(entity)) {
            return Tier.ELITE;
        }
        return Tier.NORMAL;
    }

    public static boolean isBoss(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        if (type.is(ModTags.BOSSES)) {
            return true;
        }
        if (entity instanceof EnderDragon || entity instanceof WitherBoss) {
            return true;
        }
        if (entity.getClass().getName().startsWith(GOETY_BOSS_PACKAGE)) {
            return true;
        }
        // 兜底：一些模组 Boss 没有实现 Enemy 或没有进标签。只对敌对生物使用。
        return entity instanceof Enemy
                && entity.getMaxHealth() >= VoidScytheBalance.BOSS_HEALTH_THRESHOLD;
    }

    public static boolean isElite(LivingEntity entity) {
        if (entity.getType().is(ModTags.ELITES)) {
            return true;
        }
        if (entity.getTags().contains("goetyarcana:elite")) {
            return true;
        }
        if (entity.getPersistentData().getBoolean("goetyarcana:elite")) {
            return true;
        }
        return entity instanceof Enemy
                && entity.getMaxHealth() >= VoidScytheBalance.ELITE_HEALTH_THRESHOLD;
    }

    private CombatTier() {
    }
}
