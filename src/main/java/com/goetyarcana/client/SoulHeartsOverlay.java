package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 灵魂套的额外心：用青蓝色的灵魂心覆盖血条上方多出来的那几颗，
 * 方便和原版红心区分。
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SoulHeartsOverlay {

    private static final ResourceLocation FULL =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/gui/soul_heart.png");
    private static final ResourceLocation EMPTY =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/gui/soul_heart_empty.png");
    private static final int VANILLA_HEARTS = 10;

    private SoulHeartsOverlay() {
    }

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui
                || minecraft.player.isCreative() || minecraft.player.isSpectator()) {
            return;
        }
        int hearts = SoulHeartsCache.get(minecraft.player.getId());
        if (hearts <= 0) {
            return;
        }
        // 只有穿齐灵魂套时才显示这排心
        if (!com.goetyarcana.combat.SoulArmor.isFullSet(minecraft.player)) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        for (int i = 0; i < hearts; i++) {
            // 靠右对齐、画在饥饿条上方那一行：中央区域会撞到效果图标/护甲条
            int x = width / 2 + 91 - 9 - (i % 10) * 8;
            int y = height - 49 - (i / 10) * 10;
            graphics.blit(FULL, x, y + 1, 0, 0, 9, 9, 9, 9);
            graphics.blit(FULL, x, y + 1, 0, 0, 9, 9, 9, 9);
        }
    }
}
