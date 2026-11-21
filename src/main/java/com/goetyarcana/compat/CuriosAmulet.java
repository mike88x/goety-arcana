package com.goetyarcana.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Curios 的访问桥：只在 Curios 已加载时被调用，避免其它环境直接加载 Curios 类。
 */
public final class CuriosAmulet {

    private CuriosAmulet() {
    }

    /** 身上是否穿着 Goety 的长袍类饰品（长袍会盖住腿，避免护腿穿模）。 */
    public static boolean hasRobe(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> {
                    for (var entry : handler.getCurios().values()) {
                        var stacks = entry.getStacks();
                        for (int i = 0; i < stacks.getSlots(); i++) {
                            ItemStack stack = stacks.getStackInSlot(i);
                            if (stack.isEmpty()) {
                                continue;
                            }
                            var id = net.minecraftforge.registries.ForgeRegistries.ITEMS
                                    .getKey(stack.getItem());
                            if (id != null && id.getPath().contains("robe")) {
                                return true;
                            }
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    public static boolean isWorn(Player player, net.minecraft.world.item.Item item) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.isEquipped(item))
                .orElse(false);
    }
}
