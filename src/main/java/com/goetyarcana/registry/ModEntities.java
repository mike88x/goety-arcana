package com.goetyarcana.registry;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.entity.ExecutionSlashEntity;
import com.goetyarcana.entity.ScytheWaveEntity;
import com.goetyarcana.entity.SoulBurstEntity;
import com.goetyarcana.entity.VoidBoltEntity;
import com.goetyarcana.entity.VoidExplosionEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GoetyArcanaMod.MOD_ID);

    /** 灵魂弹 */
    public static final RegistryObject<EntityType<SoulBurstEntity>> SOUL_BURST =
            ENTITY_TYPES.register("soul_burst", () -> EntityType.Builder
                    .<SoulBurstEntity>of(SoulBurstEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    // 每 tick 同步一次位置，高速飞行时客户端插值才不会一跳一跳
                    .updateInterval(1)
                    .build(GoetyArcanaMod.MOD_ID + ":soul_burst"));

    /** 镰刀剑气（以玩家为中心扩散的扇形能量环） */
    public static final RegistryObject<EntityType<ScytheWaveEntity>> SCYTHE_WAVE =
            ENTITY_TYPES.register("scythe_wave", () -> EntityType.Builder
                    .<ScytheWaveEntity>of(ScytheWaveEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(GoetyArcanaMod.MOD_ID + ":scythe_wave"));

    /** 虚空弩矢 */
    public static final RegistryObject<EntityType<VoidBoltEntity>> VOID_BOLT =
            ENTITY_TYPES.register("void_bolt", () -> EntityType.Builder
                    .<VoidBoltEntity>of(VoidBoltEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build(GoetyArcanaMod.MOD_ID + ":void_bolt"));

    /** 虚空爆炸表现动画 */
    public static final RegistryObject<EntityType<VoidExplosionEntity>> VOID_EXPLOSION =
            ENTITY_TYPES.register("void_explosion", () -> EntityType.Builder
                    .<VoidExplosionEntity>of(VoidExplosionEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(GoetyArcanaMod.MOD_ID + ":void_explosion"));

    /** 处决 debuff 结束时的专属斩击动画 */
    public static final RegistryObject<EntityType<ExecutionSlashEntity>> EXECUTION_SLASH =
            ENTITY_TYPES.register("execution_slash", () -> EntityType.Builder
                    .<ExecutionSlashEntity>of(ExecutionSlashEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(GoetyArcanaMod.MOD_ID + ":execution_slash"));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}


