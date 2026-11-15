package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

/**
 * 虚空之眼的分层图标：1/2/3 层各一张 18x18，HUD 和物品栏都按 amplifier 选图。
 *
 * <p>原版一个 effect 只有一张图标；这里用 Forge 的
 * {@link IClientMobEffectExtensions} 接管绘制（返回 true 表示不再画默认图标）。</p>
 */
public final class VoidEyeIcon implements IClientMobEffectExtensions {

    public static final VoidEyeIcon INSTANCE = new VoidEyeIcon();

    private static final ResourceLocation[] ICONS = {
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/mob_effect/void_eye.png"),
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/mob_effect/void_eye_2.png"),
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/mob_effect/void_eye_3.png"),
    };

    private VoidEyeIcon() {
    }

    private static ResourceLocation icon(MobEffectInstance instance) {
        int stack = Mth.clamp(instance.getAmplifier(), 0, ICONS.length - 1);
        return ICONS[stack];
    }

    @Override
    public boolean renderInventoryIcon(MobEffectInstance instance,
                                       EffectRenderingInventoryScreen<?> screen,
                                       GuiGraphics guiGraphics, int x, int y, int blitOffset) {
        // 物品栏效果列表的默认图标是在 (x, y + 7) 画的，钩子给的 y 是行首，所以要自己 +7。
        guiGraphics.blit(icon(instance), x, y + 7, 0, 0, 18, 18, 18, 18);
        return true;
    }

    @Override
    public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics guiGraphics,
                                 int x, int y, float z, float alpha) {
        // 原版 HUD 是在 24x24 的底图里按 (x+3, y+3) 画 18x18 图标的，这里跟它对齐。
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.blit(icon(instance), x + 3, y + 3, 0, 0, 18, 18, 18, 18);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        return true;
    }
}
