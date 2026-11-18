package com.goetyarcana.registry;

import com.goetyarcana.GoetyArcanaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 本模组自己的创造模式标签页。
 *
 * <p>以后每新增一个物品，记得同时加进 {@code displayItems}，
 * 这样新标签页始终收纳全部模组物品。</p>
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GoetyArcanaMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN =
            CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.goetyarcana"))
                    .icon(() -> new ItemStack(ModItems.VOID_SCYTHE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.VOID_SCYTHE.get());
                        output.accept(ModItems.VOID_CROSSBOW.get());
                        output.accept(ModItems.VOID_CROSSBOW_BOLT.get());
                        output.accept(ModItems.SOUL_BURST_FOCUS.get());
                        output.accept(ModItems.BRAMBLE_BIND_FOCUS.get());
                        output.accept(ModItems.VOID_RUPTURE_FOCUS.get());
                        output.accept(ModItems.VOID_SHARD.get());
                        output.accept(ModItems.VOID_CREEPER_HEAD.get());
                        output.accept(ModItems.VOID_AMULET.get());
                        output.accept(ModItems.VOID_CREEPER_SPAWN_EGG.get());
                        output.accept(ModItems.VOID_CREEPER_SERVANT_SPAWN_EGG.get());
                        output.accept(ModItems.ENCHANTED_VOID_CREEPER_SERVANT_SPAWN_EGG.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        CREATIVE_TABS.register(modBus);
    }
}


