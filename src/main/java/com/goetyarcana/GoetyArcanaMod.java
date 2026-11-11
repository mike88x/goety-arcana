package com.goetyarcana;

import com.goetyarcana.network.ModNetwork;
import com.goetyarcana.registry.ModCreativeTabs;
import com.goetyarcana.registry.ModEffects;
import com.goetyarcana.registry.ModEntities;
import com.goetyarcana.registry.ModItems;
import com.goetyarcana.registry.ModParticles;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

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

    public GoetyArcanaMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modBus);
        ModEntities.register(modBus);
        ModEffects.register(modBus);
        ModParticles.register(modBus);
        ModCreativeTabs.register(modBus);
        ModNetwork.register();
    }
}
