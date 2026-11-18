package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.VoidBlastBalance;
import com.goetyarcana.entity.VoidCreeper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 虚空苦力怕对视警告：屏幕边缘浮起紫色的滤镜，越接近发怒越浓，看得越久越压抑。
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VoidCreeperWarnOverlay {

    private VoidCreeperWarnOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        float strongest = 0.0F;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof VoidCreeper creeper && !creeper.isAngry()) {
                float progress = creeper.getAlert() / (float) VoidBlastBalance.VOID_CREEPER_STARE_TICKS;
                strongest = Math.max(strongest, progress);
            }
        }
        if (strongest <= 0.05F) {
            return;
        }
        float pulse = 0.75F + 0.25F * Mth.sin(minecraft.player.tickCount * 0.15F);
        float strength = Mth.clamp(strongest * 1.1F, 0.0F, 1.0F) * pulse;
        int alpha = (int) (28 + 96 * strength) << 24;
        int purple = alpha | 0x3A1A5C;
        int deep = alpha | 0x1A0A2E;

        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int edge = Math.max(40, (int) (height * 0.28F));

        graphics.fillGradient(0, 0, width, edge, purple, 0x00000000);
        graphics.fillGradient(0, height - edge, width, height, 0x00000000, deep);
        graphics.fillGradient(0, 0, edge, height, purple, 0x00000000);
        graphics.fillGradient(width - edge, 0, width, height, 0x00000000, deep);
    }
}
