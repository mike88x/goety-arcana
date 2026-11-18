package com.goetyarcana.combat;

import com.goetyarcana.GoetyArcanaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * 判断一个生物算不算「精英 / BOSS」，用的是我们自己的实体类型标签
 * （`data/goetyarcana/tags/entity_types/{elites,bosses}.json`）。
 */
public final class VoidBlastTags {

    private static final TagKey<EntityType<?>> BOSSES =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(GoetyArcanaMod.MOD_ID, "bosses"));
    private static final TagKey<EntityType<?>> ELITES =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(GoetyArcanaMod.MOD_ID, "elites"));

    private VoidBlastTags() {
    }

    public static boolean isEliteOrBoss(LivingEntity entity) {
        var holder = entity.getType().builtInRegistryHolder();
        return holder.is(BOSSES) || holder.is(ELITES);
    }
}
