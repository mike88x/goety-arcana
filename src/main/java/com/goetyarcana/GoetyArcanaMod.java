package com.goetyarcana;

import com.goetyarcana.network.ModNetwork;
import com.goetyarcana.registry.ModCreativeTabs;
import com.goetyarcana.registry.ModEffects;
import com.goetyarcana.registry.ModEntities;
import com.goetyarcana.registry.ModBlocks;
import com.goetyarcana.registry.ModItems;
import com.goetyarcana.registry.ModParticles;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Goety Arcana —— 诡厄巫法（Goety）附属模组。
 *
 * <p>当前物品：</p>
 * <ul>
 *   <li>裂魂聚晶（虚空系聚晶，固定 + 百分比伤害）；</li>
 *   <li>虚空镰刀（创造模式专属武器：360° 横扫、蓄力剑气、虚空之蚀、处决）。</li>
 * </ul>
 */
@Mod(GoetyArcanaMod.MOD_ID)
public class GoetyArcanaMod {

    public static final String MOD_ID = "goetyarcana";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public GoetyArcanaMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modBus);
        ModBlocks.register(modBus);
        ModEntities.register(modBus);
        ModEffects.register(modBus);
        ModParticles.register(modBus);
        ModCreativeTabs.register(modBus);
        ModNetwork.register();
        // 注意：SpawnPlacements 必须在 FMLCommonSetup（实体已注册）里登记，
        // 在构造函数里直接 get() 会抛 "Registry Object not present"。
        modBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> net.minecraft.world.entity.SpawnPlacements.register(
                        ModEntities.VOID_CREEPER.get(),
                        net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        com.goetyarcana.entity.VoidCreeper::checkSpawnRules)));
        // 生物实体必须在这里登记属性表，否则构造时 AttributeSupplier 为 null 直接崩
        modBus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event) ->
                event.put(ModEntities.VOID_CREEPER_SERVANT.get(),
                        com.goetyarcana.entity.VoidCreeperServant.createAttributes().build()));
        modBus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event) ->
                event.put(ModEntities.ENCHANTED_VOID_CREEPER_SERVANT.get(),
                        com.goetyarcana.entity.EnchantedVoidCreeperServant.createAttributes().build()));
        modBus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event) ->
                event.put(ModEntities.VOID_CREEPER.get(),
                        com.goetyarcana.entity.VoidCreeper.createAttributes().build()));
    }
}
