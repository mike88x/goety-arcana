package com.goetyarcana.combat;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 精英 / BOSS 身上的「砰砰诅咒」记账：下一次虚空苦力怕仆从爆炸伤害 +50%。
 *
 * <p>用 gameTick 记录过期时间（10 分钟），按 UUID 存；每次爆炸取用时消耗一层。
 * 普通怪物不走这里——它们吃 effect，生命上限直接压到 1。</p>
 */
public final class VoidBlastCurseTracker {

    /** UUID -> (过期 gameTick, 加成倍率)。 */
    private static final Map<UUID, int[]> EXPIRY = new HashMap<>();
    private static final Map<UUID, Float> BONUS = new HashMap<>();

    private VoidBlastCurseTracker() {
    }

    public static void mark(LivingEntity entity, int currentTick, int durationTicks, float bonus) {
        EXPIRY.put(entity.getUUID(), new int[]{currentTick + durationTicks});
        BONUS.put(entity.getUUID(), bonus);
    }

    public static boolean isCursed(LivingEntity entity, int currentTick) {
        int[] expiry = EXPIRY.get(entity.getUUID());
        return expiry != null && expiry[0] >= currentTick;
    }

    /** 取走一层诅咒并返回加成（0.5 = +50%），没有就返回 0。 */
    public static float consume(LivingEntity entity, int currentTick) {
        int[] expiry = EXPIRY.get(entity.getUUID());
        if (expiry == null || expiry[0] < currentTick) {
            return 0.0F;
        }
        EXPIRY.remove(entity.getUUID());
        Float bonus = BONUS.remove(entity.getUUID());
        return bonus != null ? bonus : com.goetyarcana.VoidBlastBalance.CURSE_BLAST_BONUS;
    }

    public static void clear(LivingEntity entity) {
        EXPIRY.remove(entity.getUUID());
        BONUS.remove(entity.getUUID());
    }

    public static void prune(int currentTick) {
        Iterator<Map.Entry<UUID, int[]>> it = EXPIRY.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue()[0] < currentTick) {
                it.remove();
            }
        }
    }
}
