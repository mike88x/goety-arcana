package com.goetyarcana.combat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * 检测是否穿着 Goety 的「虚空长袍」。
 *
 * <p>这里不直接 import Goety 的类：Goety 若没装（或换了版本、改了注册名），
 * 只是这条联动失效，而不是崩在类初始化上。物品通过 {@link ForgeRegistries#ITEMS}
 * 按 id 查一次并缓存。</p>
 *
 * <p>虚空长袍既能穿在胸甲槽，也能放进 Curios 饰品栏；两处都算数。
 * Curios 相关代码在 {@link CuriosVoidRobe}，只有装了 Curios 才会被加载。</p>
 */
public final class VoidRobe {

    private static final ResourceLocation VOID_ROBE = new ResourceLocation("goety", "void_robe");

    private static boolean resolved;
    @Nullable
    private static Item robeItem;

    private static boolean curiosChecked;
    private static boolean curiosLoaded;

    private VoidRobe() {
    }

    /** 四个护甲槽或 Curios 饰品栏里任意一处装着虚空长袍即算数。 */
    public static boolean isWorn(LivingEntity entity) {
        Item robe = robe();
        if (robe == null) {
            return false;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            ItemStack worn = entity.getItemBySlot(slot);
            if (!worn.isEmpty() && worn.is(robe)) {
                return true;
            }
        }
        return curio(entity, robe);
    }

    private static boolean curio(LivingEntity entity, Item robe) {
        if (!curiosChecked) {
            curiosLoaded = ModList.get().isLoaded("curios");
            curiosChecked = true;
        }
        // 未装 Curios 时不会触发 CuriosVoidRobe 的类加载。
        return curiosLoaded && CuriosVoidRobe.isWorn(entity, robe);
    }

    @Nullable
    private static Item robe() {
        if (!resolved) {
            robeItem = ForgeRegistries.ITEMS.getValue(VOID_ROBE);
            resolved = true;
        }
        return robeItem;
    }
}
