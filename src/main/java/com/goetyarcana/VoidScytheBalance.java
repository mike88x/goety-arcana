package com.goetyarcana;

/**
 * 虚空镰刀的全部数值。
 *
 * <p>这里集中放数值，方便后续平衡；修改后重新 build 即可。</p>
 *
 * <p>重要的两个“百分比”不是固定伤害：</p>
 * <ul>
 *   <li>剑气：目标最大生命值 × 5%~15%（按蓄力比例线性提升），没有基础伤害；</li>
 *   <li>处决：普通敌人直接消灭；精英损失 10% 最大生命值；Boss 损失 20% 最大生命值。</li>
 * </ul>
 */
public final class VoidScytheBalance {

    /* ------------------------------------------------------------------ */
    /* 近战                                                                */
    /* ------------------------------------------------------------------ */

    /** 物品面板攻击伤害（玩家空手基础 1 点 + 武器 12 点）。 */
    public static final float ATTACK_DAMAGE = 13.0F;

    /** 物品面板攻击速度（玩家基础 4.0 + 武器 -3.1）。 */
    public static final float ATTACK_SPEED = 0.9F;

    /** 360° 横扫的水平半径（格）。 */
    public static final double MELEE_RADIUS = 4.5D;

    /** 360° 横扫允许的垂直高度差（格）。 */
    public static final double MELEE_HEIGHT = 2.5D;

    /** 近战攻击一次消耗的耐久。 */
    public static final int MELEE_DURABILITY_COST = 1;

    /** 虚空镰刀耐久。 */
    public static final int DURABILITY = 2031;

    /** 附魔能力。 */
    public static final int ENCHANTABILITY = 18;

    /* ------------------------------------------------------------------ */
    /* 虚空之蚀 / 处决                                                     */
    /* ------------------------------------------------------------------ */

    /** 虚空之蚀持续时间，100 tick = 5 秒。 */
    public static final int VOID_EROSION_TICKS = 100;

    /** 左键命中时，施加虚空之蚀 I 的概率；剩下的概率施加 II。 */
    public static final float VOID_EROSION_I_CHANCE = 0.70F;

    /** 左键或剑气施加处决的概率。 */
    public static final float EXECUTION_CHANCE = 0.05F;

    /** 处决 debuff 持续时间，100 tick = 5 秒。 */
    public static final int EXECUTION_TICKS = 100;

    /** 处决 debuff 结束后，专属斩击动画的蓄势时间（tick），随后才真正结算。 */
    public static final int EXECUTION_ANIMATION_TICKS = 12;

    /** 处决结束时，精英怪损失的最大生命值比例。 */
    public static final float EXECUTION_ELITE_PERCENT = 0.10F;

    /** 处决结束时，Boss 损失的最大生命值比例。 */
    public static final float EXECUTION_BOSS_PERCENT = 0.20F;

    /**
     * 没有安装额外 Boss 标签时，用最大生命值兜底判定 Boss。
     * Goety 自己的 Boss 类、原版 Boss 接口和 entity_types 标签会优先命中。
     */
    public static final float BOSS_HEALTH_THRESHOLD = 200.0F;

    /** 没有额外精英标签时，用最大生命值兜底判定精英。 */
    public static final float ELITE_HEALTH_THRESHOLD = 60.0F;

    /* ------------------------------------------------------------------ */
    /* 蓄力                                                                */
    /* ------------------------------------------------------------------ */

    /** 最短可释放蓄力时间（tick）。低于这个时间松开不会释放剑气。 */
    public static final int MIN_CHARGE_TICKS = 5;

    /** 蓄满所需时间（tick），30 tick = 1.5 秒。 */
    public static final int FULL_CHARGE_TICKS = 30;

    /** 松开蓄力后物品的冷却（tick）。 */
    public static final int WAVE_COOLDOWN_TICKS = 25;

    /**
     * 剑气存在时间（tick）。
     *
     * <p>100 tick = 5 秒；这 5 秒内剑气会一直向前飞，不会撞到方块就消失。</p>
     */
    public static final int WAVE_LIFETIME = 100;

    /** 剑气前进速度（格/tick）。1.0 与死神之镰的剑气一致。 */
    public static final double WAVE_SPEED = 1.0D;

    /** 剑气生成点沿视线前移的距离（格），避免贴脸时看不见。 */
    public static final double WAVE_SPAWN_OFFSET = 1.0D;

    /** 最低蓄力的剑气宽度（格，横向）。 */
    public static final double WAVE_MIN_WIDTH = 2.4D;

    /** 满蓄力的剑气宽度（格，横向）。 */
    public static final double WAVE_MAX_WIDTH = 6.0D;

    /** 最低蓄力的剑气高度（格，纵向）。 */
    public static final double WAVE_MIN_HEIGHT = 2.2D;

    /** 满蓄力的剑气高度（格，纵向）。 */
    public static final double WAVE_MAX_HEIGHT = 5.0D;

    /**
     * 剑气命中判定的厚度（格，沿飞行方向）。
     *
     * <p>剑气每 tick 前进 1 格；判定盒会在上一 tick 与当前 tick 的位置之间
     * 取并集，所以即使目标站在两帧之间也不会被“穿透”漏掉。</p>
     */
    public static final double WAVE_HIT_DEPTH = 1.25D;

    /** 满蓄力时剑气命中造成目标最大生命值的比例。 */
    public static final float WAVE_MAX_PERCENT = 0.15F;

    /** 最低可释放蓄力时剑气命中造成目标最大生命值的比例。 */
    public static final float WAVE_MIN_PERCENT = 0.05F;

    private VoidScytheBalance() {
    }
}
