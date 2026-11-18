package com.goetyarcana.combat;

import com.goetyarcana.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 虚空护身符：基础版「随身携带即生效」。
 *
 * <p>暂时不注册成 Curio（那需要硬依赖 Curios API），先支持放在背包任意位置；
 * 后续做成 Curio 饰品时，只需要把 {@link #isCarried(Player)} 改成读饰品栏。</p>
 */
public final class VoidAmulet {

    /** 伤害转化的比例。 */
    public static final float CONVERT_RATIO = 0.42F;

    private VoidAmulet() {
    }

    public static boolean isCarried(Player player) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("curios")
                && com.goetyarcana.compat.CuriosAmulet.isWorn(player,
                        ModItems.VOID_AMULET.get())) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.VOID_AMULET.get())) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(ModItems.VOID_AMULET.get())) {
                return true;
            }
        }
        return player.getInventory().getSelected().is(ModItems.VOID_AMULET.get());
    }
}
