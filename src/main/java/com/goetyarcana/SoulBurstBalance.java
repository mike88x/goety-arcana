package com.goetyarcana;

/**
 * 数值集中放在这里，改完重新 build 即可生效。
 *
 * 伤害公式：
 *     最终伤害 = FIXED_DAMAGE + 目标最大生命值 * PERCENT_OF_MAX_HEALTH + 聚晶「强效」附魔加成
 *
 * 默认：6 + 最大生命值 * 5%
 *   - 20 血的普通怪：6 + 1 = 7 点
 *   - 100 血的精英：6 + 5 = 11 点
 *   - 300 血的 Boss：6 + 15 = 21 点（**不受 Goety Boss 伤害上限削减**）
 */
public final class SoulBurstBalance {

    /** 固定伤害（1 点 = 半颗心） */
    public static final float FIXED_DAMAGE = 6.0F;

    /**
     * 目标最大生命值的百分比伤害，0.075F = 7.5%。
     * 因为伤害已经能突破 Boss 上限，所以取一个介于 5%~10% 之间的平衡值。
     */
    public static final float PERCENT_OF_MAX_HEALTH = 0.075F;

    /** 每级「强效」附魔额外增加的固定伤害 */
    public static final float POTENCY_BONUS = 1.0F;

    /** 施法消耗的灵魂能量 */
    public static final int SOUL_COST = 35;

    /** 吟唱时间（tick），20 tick = 1 秒 */
    public static final int CAST_DURATION = 10;

    /** 冷却（tick），30 tick = 1.5 秒 */
    public static final int COOLDOWN = 30;

    /** 灵魂弹飞行速度（格/tick）。3.6 ≈ 72 格/秒，比箭快得多 */
    public static final double PROJECTILE_SPEED = 3.6D;
    /** 有追踪（虚空套）时降低初速，转弯跟得上。 */
    public static final double HOMING_SPEED = 2.0D;

    /** 射弹出生点相对眼睛沿视线方向的前移距离，0.6 ≈ 正好从准星处飞出 */
    public static final double SPAWN_OFFSET = 0.6D;

    /** 虚空魔杖施放时的爆炸半径（格） */
    public static final double VOID_AOE_RADIUS = 3.0D;

    /** 虚空魔杖施放时，范围内其他目标吃到的伤害比例 */
    public static final float VOID_AOE_MULTIPLIER = 0.5F;

    /** 灵魂弹最长存活时间（tick） */
    public static final int PROJECTILE_LIFETIME = 60;

    private SoulBurstBalance() {
    }
}
