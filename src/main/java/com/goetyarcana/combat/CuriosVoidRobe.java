package com.goetyarcana.combat;

import com.goetyarcana.GoetyArcanaMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.items.IItemHandler;

import java.lang.reflect.Method;

/**
 * 在 Curios 饰品栏里找虚空长袍。
 *
 * <p>Curios 对这个模组是可选依赖，所以全程反射、编译期不依赖 Curios：
 * 只有 {@link VoidRobe} 确认 curios 已加载时才会加载本类，缺 Curios 时
 * 既不会 NoClassDefFoundError，也不会拖慢正常装填。</p>
 *
 * <p>反射调用链（Curios 5.x）：
 * {@code CuriosApi.getCuriosInventory(LivingEntity)} → LazyOptional →
 * {@code ICuriosItemHandler.getEquippedCurios()} → Forge {@link IItemHandler}。</p>
 */
final class CuriosVoidRobe {

    private static final String API_CLASS = "top.theillusivec4.curios.api.CuriosApi";
    private static final String HANDLER_CLASS = "top.theillusivec4.curios.api.type.capability.ICuriosItemHandler";
    private static final String LAZY_OPTIONAL_CLASS = "net.minecraftforge.common.util.LazyOptional";

    private static Method getCuriosInventory;
    private static Method lazyOrElse;
    private static Method getEquippedCurios;
    private static boolean broken;

    private CuriosVoidRobe() {
    }

    static boolean isWorn(LivingEntity entity, Item robe) {
        if (broken) {
            return false;
        }
        try {
            if (getCuriosInventory == null) {
                resolve();
            }
            Object lazy = getCuriosInventory.invoke(null, entity);
            if (lazy == null) {
                return false;
            }
            Object handler = lazyOrElse.invoke(lazy, (Object) null);
            if (handler == null) {
                return false;
            }
            Object equipped = getEquippedCurios.invoke(handler);
            if (!(equipped instanceof IItemHandler stacks)) {
                return false;
            }
            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                // 只看已装备的饰品槽，不看时装槽。
                if (stacks.getStackInSlot(slot).is(robe)) {
                    return true;
                }
            }
            return false;
        } catch (Throwable throwable) {
            broken = true;
            GoetyArcanaMod.LOGGER.warn("Curios integration disabled: void robe curio slot will not count",
                    throwable);
            return false;
        }
    }

    private static void resolve() throws ReflectiveOperationException {
        getCuriosInventory = Class.forName(API_CLASS)
                .getMethod("getCuriosInventory", LivingEntity.class);
        lazyOrElse = Class.forName(LAZY_OPTIONAL_CLASS)
                .getMethod("orElse", Object.class);
        getEquippedCurios = Class.forName(HANDLER_CLASS)
                .getMethod("getEquippedCurios");
    }
}
