package com.goetyarcana.registry;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.item.SoulBurstFocus;
import com.goetyarcana.item.VoidScytheItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GoetyArcanaMod.MOD_ID);

    /** 裂魂聚晶 */
    public static final RegistryObject<Item> SOUL_BURST_FOCUS =
            ITEMS.register("soul_burst_focus", SoulBurstFocus::new);

    /** 虚空镰刀 */
    public static final RegistryObject<Item> VOID_SCYTHE =
            ITEMS.register("void_scythe", VoidScytheItem::new);

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
