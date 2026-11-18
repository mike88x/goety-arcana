package com.goetyarcana;

/**
 * 虚空崩爆聚晶 + 虚空苦力怕仆从的所有数值（测试版，后续会调）。
 */
public final class VoidBlastBalance {

    /** 灵魂能量消耗（测试值）。 */
    public static final int SOUL_COST = 20;
    /** 吟唱 tick（1 秒）。 */
    public static final int CAST_DURATION = 30;
    /** 冷却 tick（10 秒）。 */
    public static final int SPELL_COOLDOWN = 200;

    /** 仆从生命值。 */
    public static final double SERVANT_HEALTH = 10.0D;
    /** 仆从护甲。 */
    public static final double SERVANT_ARMOR = 6.0D;
    /** 仆从护甲韧性。 */
    public static final double SERVANT_TOUGHNESS = 6.0D;
    /** 索敌 / 跟随半径。 */
    public static final double SERVANT_RANGE = 20.0D;

    /** 看见敌人后先「瞬移准备」2 秒。 */
    public static final int TELEPORT_WINDUP = 40;
    /** 瞬移过去后 1 秒爆炸。 */
    public static final int FUSE_TICKS = 20;

    /** 原版苦力怕在困难模式下贴脸爆炸约 49 点，取 50%。 */
    public static final float EXPLOSION_DAMAGE = 24.5F;
    /** 爆炸伤害半径。 */
    public static final double EXPLOSION_RADIUS = 5.0D;
    /** 边缘衰减到几成。 */
    public static final double EXPLOSION_MIN_FACTOR = 0.4D;

    /** 砰砰诅咒持续时间：10 分钟（精英 / BOSS）。 */
    public static final int CURSE_TICKS = 12000;
    /** 普通怪物的一级诅咒时长：1 分钟。 */
    public static final int CURSE_TICKS_NORMAL = 1200;
    /** 精英 / BOSS 每层诅咒让下一次爆炸伤害提升的比例。 */
    public static final float CURSE_BLAST_BONUS = 0.5F;
    /** 附魔版诅咒给精英 / BOSS 的加成（+100%）。 */
    public static final float CURSE_BLAST_BONUS_ENCHANTED = 1.0F;

    /** 附魔虚空苦力怕：原版闪电苦力怕困难模式最大伤害的 75%。 */
    public static final float ENCHANTED_EXPLOSION_DAMAGE = 73.5F;
    /** 用虚空魔杖施法时，每只替换成附魔版的概率。 */
    public static final float ENCHANTED_CHANCE = 0.3F;
    /** 一次施法召唤数量：默认 3，穿虚空长袍 6。 */
    public static final int SUMMON_COUNT = 3;
    public static final int SUMMON_COUNT_ROBE = 6;

    /** 强制吸引仇恨的判定半径，以及刷新间隔。 */
    public static final double TAUNT_RANGE = 24.0D;
    public static final int TAUNT_INTERVAL = 10;

    // ------------------------------------------------------------ 自然生成的虚空苦力怕

    /** 生命 / 护甲 / 韧性。 */
    public static final double VOID_CREEPER_HEALTH = 42.0D;
    public static final double VOID_CREEPER_ARMOR = 6.0D;
    public static final double VOID_CREEPER_TOUGHNESS = 7.0D;

    /** 盯着它看多久会彻底愤怒（10 秒）。 */
    public static final int VOID_CREEPER_STARE_TICKS = 200;
    /** 愤怒后瞬移过去、再引信 2.5 秒爆炸。 */
    public static final int VOID_CREEPER_FUSE_TICKS = 50;
    /** 瞬移 + 引信这段的伤害减免（50%）。 */
    public static final float VOID_CREEPER_CHARGE_RESIST = 0.5F;

    /** 爆炸伤害：原版苦力怕对应难度最大伤害的 60%。 */
    public static final float VOID_CREEPER_DAMAGE_EASY = 14.7F;
    public static final float VOID_CREEPER_DAMAGE_NORMAL = 20.6F;
    public static final float VOID_CREEPER_DAMAGE_HARD = 29.4F;

    /** 虚空碎片掉落概率（基础 5%，每级抢夺 +1%）。 */
    public static final float VOID_SHARD_CHANCE = 0.10F;
    public static final float VOID_SHARD_LOOTING_BONUS = 0.01F;

    private VoidBlastBalance() {
    }
}
