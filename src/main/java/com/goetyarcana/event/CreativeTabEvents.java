package com.goetyarcana.event;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 把聚晶放进 Goety 自己的「诡厄巫法：聚晶」创造模式标签页。
 *
 * 该标签页在 Goety 的 ModCreativeTab 里以 "goety_focus" 注册在 goety 命名空间下，
 * 所以资源键是 goety:goety_focus。
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CreativeTabEvents {

    private static final ResourceKey<CreativeModeTab> GOETY_FOCUS_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            new ResourceLocation("goety", "goety_focus"));

    private CreativeTabEvents() {
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(GOETY_FOCUS_TAB)) {
            event.accept(ModItems.SOUL_BURST_FOCUS.get());
        }
    }
}
