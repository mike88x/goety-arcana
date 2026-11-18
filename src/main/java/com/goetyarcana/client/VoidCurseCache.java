package com.goetyarcana.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 客户端侧的「砰砰诅咒」缓存。
 *
 * <p>使徒这类 BOSS 会在收到效果的同一 tick 里把它清掉（服务端日志：`added=true hasEffect=false`），
 * HUD/tooltip 永远看不到。所以诅咒状态由服务端单独发包同步，头顶标记直接读这份缓存。</p>
 */
public final class VoidCurseCache {

    private static final Map<Integer, Long> EXPIRY = new HashMap<>();

    private VoidCurseCache() {
    }

    public static void accept(int entityId, int durationTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = minecraft.level != null ? minecraft.level.getGameTime() : 0L;
        EXPIRY.put(entityId, now + durationTicks);
    }

    public static boolean isCursed(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        Long expiry = EXPIRY.get(entity.getId());
        return expiry != null && expiry >= minecraft.level.getGameTime();
    }

    public static void prune() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            EXPIRY.clear();
            return;
        }
        long now = minecraft.level.getGameTime();
        Iterator<Map.Entry<Integer, Long>> it = EXPIRY.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() < now) {
                it.remove();
            }
        }
    }
}
