package com.goetyarcana.registry;

import com.goetyarcana.GoetyArcanaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * 本模组使用的标签。
 *
 * <p>Boss / 精英判定优先看 {@code goetyarcana:bosses} 与
 * {@code goetyarcana:elites}，这样整合包可以通过数据包给其它模组的怪加上标记；
 * 没有标签时再使用最大生命值兜底。</p>
 */
public final class ModTags {

    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(
            Registries.ENTITY_TYPE,
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "bosses"));

    public static final TagKey<EntityType<?>> ELITES = TagKey.create(
            Registries.ENTITY_TYPE,
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "elites"));

    private ModTags() {
    }
}
