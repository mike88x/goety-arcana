package com.goetyarcana.combat;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 服务端的「藤毒」登记表。
 *
 * <p>为什么不用 effect 判断：部分 BOSS 会周期性清除身上的负面效果，或者根本不吃
 * 自定义 effect。藤毒的伤害本来就直接结算，但**禁疗**需要一个不依赖 effect 的判定，
 * 所以这里按 UUID 记一份「到某个 gameTick 之前中毒」。</p>
 *
 * <p>藤蔓每 tick 刷新一次登记，藤蔓结束就撤掉；`expire` 只是兜底，
 * 防止藤蔓实体异常消失后永久禁疗。</p>
 */
public final class VinePoisonTracker {

    /** 兜底过期时间：藤蔓每 tick 刷新，远小于这个值。 */
    private static final int GRACE_TICKS = 40;

    private static final Map<UUID, Integer> POISONED = new HashMap<>();

    private VinePoisonTracker() {
    }

    public static void mark(LivingEntity entity, int currentTick) {
        POISONED.put(entity.getUUID(), currentTick + GRACE_TICKS);
    }

    public static void unmark(LivingEntity entity) {
        POISONED.remove(entity.getUUID());
    }

    public static boolean isPoisoned(LivingEntity entity, int currentTick) {
        Integer expiry = POISONED.get(entity.getUUID());
        return expiry != null && expiry >= currentTick;
    }

    /** 每 200 tick 清一次过期条目。 */
    public static void prune(int currentTick) {
        Iterator<Map.Entry<UUID, Integer>> it = POISONED.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() < currentTick) {
                it.remove();
            }
        }
    }
}
