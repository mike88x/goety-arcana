package com.goetyarcana.client;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端缓存：实体 id → 灵魂心数量。
 */
public final class SoulHeartsCache {

    private static final Map<Integer, Integer> HEARTS = new HashMap<>();

    private SoulHeartsCache() {
    }

    public static void set(int entityId, int hearts) {
        HEARTS.put(entityId, hearts);
    }

    public static int get(int entityId) {
        return HEARTS.getOrDefault(entityId, 0);
    }

    public static void clear() {
        HEARTS.clear();
    }
}
