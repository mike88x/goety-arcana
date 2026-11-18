package com.goetyarcana;

/**
 * 荆棘藤缚的所有数值（测试用，后续会调）。
 */
public final class BrambleBindBalance {

    /** 灵魂能量消耗：15。 */
    public static final int SOUL_COST = 15;
    /** 吟唱 3 秒。 */
    public static final int CAST_DURATION = 60;
    /** 冷却 20 秒。 */
    public static final int SPELL_COOLDOWN = 400;

    /** 种子飞行速度。 */
    public static final float SEED_VELOCITY = 1.1F;
    /** 种子落地后没怪时的等待时间：10 秒后本轮结束。 */
    public static final int SEED_LIFETIME = 200;

    /** 默认检测范围：3x3 格（半宽 1.5）。 */
    public static final double CHECK_RADIUS = 1.5D;
    /** 荒野魔杖：7x7（半宽 3.5）。 */
    public static final double STAFF_RADIUS = 3.5D;
    /** 荒野长袍：9x9（半宽 4.5），覆盖魔杖的范围。 */
    public static final double ROBE_RADIUS = 4.5D;

    /** 一次最多缠多少个目标。 */
    public static final int MAX_TARGETS = 8;
    /** 荒野魔杖：最多缠 16 个目标。 */
    public static final int STAFF_MAX_TARGETS = 16;
    /** 默认缠绕持续时间：5 秒。 */
    public static final int ENTANGLE_TICKS = 100;
    /** 荒野魔杖：15 秒。 */
    public static final int STAFF_ENTANGLE_TICKS = 300;

    /** 红刺每秒伤害。 */
    public static final float THORN_DAMAGE = 2.0F;
    /** 藤毒每秒伤害的起始值。 */
    public static final float VINE_POISON_BASE = 1.0F;
    /** 藤毒每秒伤害的递增量（第 n 秒额外加 n 倍）。 */
    public static final float VINE_POISON_STEP = 0.75F;

    private BrambleBindBalance() {
    }
}
