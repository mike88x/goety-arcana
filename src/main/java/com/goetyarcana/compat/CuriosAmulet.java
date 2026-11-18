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

    public static boolean isWorn(Player player, net.minecraft.world.item.Item item) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.isEquipped(item))
                .orElse(false);
    }
}
