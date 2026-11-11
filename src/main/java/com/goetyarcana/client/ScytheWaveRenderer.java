package com.goetyarcana.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.VoidScytheBalance;
import com.goetyarcana.entity.ScytheWaveEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 虚空剑气渲染。
 *
 * <p>剑气现在是一道向前飞行的斩击，所以不再画“以玩家为中心扩散的弧形墙”，
 * 而是画一片面向镜头的月牙形能量刃：</p>
 * <ul>
 *   <li>外层：大一圈的柔光月牙；</li>
 *   <li>中层：主刃，颜色偏紫；</li>
 *   <li>内层：接近白色的高光刃芯。</li>
 * </ul>
 *
 * <p>三层叠加 + 自定义虚空火拖尾，飞行时始终保持可读的月牙轮廓；
 * 蓄力越高，月牙的宽度/高度越大。</p>
 */
public class ScytheWaveRenderer extends EntityRenderer<ScytheWaveEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/entity/scythe_wave.png");
    private static final int FULL_LIGHT = 0xF000F0;

    public ScytheWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ScytheWaveEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(ScytheWaveEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float tick = entity.tickCount + partialTick;
        float progress = Mth.clamp(tick / (float) VoidScytheBalance.WAVE_LIFETIME, 0.0F, 1.0F);
        float fade = 1.0F - progress * progress;

        float width = (float) entity.getWaveWidth();
        float height = (float) entity.getWaveHeight();
        float pulse = 1.0F + 0.035F * Mth.sin(tick * 0.9F);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));

        poseStack.pushPose();
        // 面向镜头，保证从发射者背后看也是一枚完整的月牙，而不是一条边。
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        // 轻微摆动，避免完全静止的贴片感。
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(tick * 0.18F) * 4.5F));

        drawLayer(consumer, poseStack, width * 1.35F * pulse, height * 1.30F * pulse,
                0.0F, 0.20F * fade, 125, 70, 255);
        drawLayer(consumer, poseStack, width * pulse, height * pulse,
                0.0F, 0.78F * fade, 180, 110, 255);
        drawLayer(consumer, poseStack, width * 0.58F * pulse, height * 0.92F * pulse,
                0.0F, 0.95F * fade, 238, 218, 255);

        poseStack.popPose();
    }

    private static void drawLayer(VertexConsumer consumer, PoseStack poseStack,
                                  float width, float height, float z,
                                  float alpha, int red, int green, int blue) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, z);
        poseStack.scale(width, height, 1.0F);

        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        Matrix4f matrix = poseStack.last().pose();

        // 面向 +Z 的四边形；缩放到 width/height 之后正好铺满月牙贴图。
        vertex(consumer, matrix, -0.5F, -0.5F, 0.0F, red, green, blue, a, 0.0F, 1.0F);
        vertex(consumer, matrix, 0.5F, -0.5F, 0.0F, red, green, blue, a, 1.0F, 1.0F);
        vertex(consumer, matrix, 0.5F, 0.5F, 0.0F, red, green, blue, a, 1.0F, 0.0F);
        vertex(consumer, matrix, -0.5F, 0.5F, 0.0F, red, green, blue, a, 0.0F, 0.0F);

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
                               float x, float y, float z,
                               int red, int green, int blue, int alpha,
                               float u, float v) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_LIGHT)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}
