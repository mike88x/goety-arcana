package com.goetyarcana.registry;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.item.SoulBurstFocus;
import com.goetyarcana.item.VoidBoltItem;
import com.goetyarcana.item.VoidCrossbowItem;
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

    /** 虚空之眼弩 */
    public static final RegistryObject<Item> VOID_CROSSBOW =
            ITEMS.register("void_crossbow", VoidCrossbowItem::new);

    /** 虚空弩矢 */
    public static final RegistryObject<Item> VOID_CROSSBOW_BOLT =
            ITEMS.register("void_crossbow_bolt", VoidBoltItem::new);

    /** 荆棘藤缚（荒野系） */
    public static final RegistryObject<Item> BRAMBLE_BIND_FOCUS =
            ITEMS.register("bramble_bind_focus", com.goetyarcana.item.BrambleBindFocus::new);

    /** 虚空崩爆聚晶（虚空系，召唤虚空苦力怕仆从） */
    public static final RegistryObject<Item> VOID_RUPTURE_FOCUS =
            ITEMS.register("void_rupture_focus", com.goetyarcana.item.VoidRuptureFocus::new);

    /** 虚空碎片：虚空物品的合成材料 */
    public static final RegistryObject<Item> VOID_SHARD =
            ITEMS.register("void_shard", () -> new Item(new Item.Properties()));

    /** 虚空苦力怕刷怪蛋 */
    public static final RegistryObject<Item> VOID_CREEPER_SPAWN_EGG =
            ITEMS.register("void_creeper_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.VOID_CREEPER, 0x2A1040, 0xA87CE0, new Item.Properties()));

    /** 虚空苦力怕仆从刷怪蛋 */
    public static final RegistryObject<Item> VOID_CREEPER_SERVANT_SPAWN_EGG =
            ITEMS.register("void_creeper_servant_spawn_egg",
                    () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                            ModEntities.VOID_CREEPER_SERVANT, 0x3A1858, 0x9B7AD8, new Item.Properties()));

    /** 附魔虚空苦力怕仆从刷怪蛋 */
    public static final RegistryObject<Item> ENCHANTED_VOID_CREEPER_SERVANT_SPAWN_EGG =
            ITEMS.register("enchanted_void_creeper_servant_spawn_egg",
                    () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                            ModEntities.ENCHANTED_VOID_CREEPER_SERVANT, 0x2A1040, 0x6EF0E0,
                            new Item.Properties()));

    /** 虚空苦力怕头颅（可摆放的 3D 头） */
    public static final RegistryObject<Item> VOID_CREEPER_HEAD =
            ITEMS.register("void_creeper_head", () -> new net.minecraft.world.item.BlockItem(
                    ModBlocks.VOID_CREEPER_HEAD.get(), new Item.Properties()));

    /** 虚空护身符（基础版：随身携带生效）。 */
    public static final RegistryObject<Item> VOID_AMULET =
            ITEMS.register("void_amulet", () -> new com.goetyarcana.item.VoidAmuletItem(
                    new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}


