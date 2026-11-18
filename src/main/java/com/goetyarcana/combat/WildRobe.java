package com.goetyarcana.combat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * 检测是否穿着 Goety 的「荒野长袍」（Curios 饰品）。
 *
 * <p>穿着时荆棘藤缚的范围升到 9x9，并且缠绕会附带藤毒。</p>
 */
public final class WildRobe {

    private static final ResourceLocation WILD_ROBE = new ResourceLocation("goety", "wild_robe");

    private WildRobe() {
    }

    public static boolean isWorn(LivingEntity entity) {
        return VoidRobe.isWearing(entity, WILD_ROBE);
    }
}
