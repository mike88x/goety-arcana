package com.goetyarcana.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.VoidScytheBalance;
import com.goetyarcana.entity.ScytheWaveEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 虚空剑气渲染：一道<b>横向</b>铺开的能量刃。
 *
 * <p>旧版把月牙贴片直接当广告牌面向镜头，看起来像一片圆形光斑。
 * 现在贴图本身就是横向的刃形（亮白刃芯 + 紫色刃身 + 青色刃缘），渲染时：</p>
 * <ul>
 *   <li>按实体朝向（{@code yRot} 的 tick 间插值）把刃面转到<b>垂直于飞行方向</b>；</li>
 *   <li>按俯仰角把刃面顺着视线抬起/压下，上下坡时不会变成竖直贴片；</li>
 *   <li>叠加一层沿飞行反方向的淡残影，强化“平滑掠过”的速度感；</li>
 *   <li>出现时 2.5 tick 淡入、寿命末尾按 {@code 1 - progress²} 淡出，不会突兀地闪现/消失。</li>
 * </ul>
 */
public class ScytheWaveRenderer extends EntityRenderer<ScytheWaveEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/entity/scythe_wave.png");
    private static final int FULL_LIGHT = 0xF000F0;

    /**
     * 横向刃形贴图（128×128）里真正有内容的区域。
     *
     * <p>贴图四周留了大片透明像素；如果直接按整张贴图铺满四边形，
     * 刃身会被透明留白压成一条很细的线、还会偏到下半边。这里只采样
     * 刃形所在的横向条带（y 40~96），让渲染尺寸与命中盒一致并保持居中。</p>
     */
    private static final float U0 = 0.0F;
    private static final float U1 = 1.0F;
    private static final float V0 = 40.0F / 128.0F;
    private static final float V1 = 96.0F / 128.0F;

    /** 剑气贴图是 128×1024 的 8 帧条带；实体贴图不经过图集动画，手动选帧。 */
    private static final int WAVE_FRAMES = 8;
    private static final int WAVE_FRAME_TICKS = 2;

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
        // 起飞阶段淡入，避免剑气在玩家眼前凭空出现。
        float appear = Mth.clamp(tick / 2.5F, 0.0F, 1.0F);
        float alphaScale = fade * appear;
        double distance = entity.position().distanceTo(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
        alphaScale *= Mth.clamp((float) ((distance - 1.0D) / 2.0D), 0.0F, 1.0F);
        if (alphaScale <= 0.005F) {
            return;
        }

        // entityYaw 已经是 yRotO→yRot 的插值结果；俯仰角同样手动插值，
        // 这样上下坡释放的剑气也会平滑跟随视线，而不是一帧一跳。
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        float width = (float) entity.getWaveWidth();
        float height = (float) entity.getWaveHeight();
        float pulse = 1.0F + 0.04F * Mth.sin(tick * 0.85F);
        float roll = 2.4F * Mth.sin(tick * 0.14F);

        // 手动取帧：把整张 8 帧条带的 V 坐标缩到当前帧内。
        int frame = Mth.floor(tick / WAVE_FRAME_TICKS) % WAVE_FRAMES;
        float frameV0 = (frame + V0) / WAVE_FRAMES;
        float frameV1 = (frame + V1) / WAVE_FRAMES;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));

        poseStack.pushPose();
        // 让刃面垂直于飞行方向：local +Z 指向剑气前进方向。
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        // 轻微侧倾，像被甩出去的刃，而不是完全静止的贴片。
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

        // 运动残影：沿飞行反方向留一层淡影。
        drawLayer(consumer, poseStack, width * 0.82F, height * 0.80F, -0.55F,
                0.18F * alphaScale, 150, 90, 255, frameV0, frameV1);
        // 外层柔光：比主刃大一圈，负责辉光。
        drawLayer(consumer, poseStack, width * 1.18F * pulse, height * 1.55F * pulse, -0.02F,
                0.22F * alphaScale, 118, 64, 255, frameV0, frameV1);
        // 主刃：横向宽、纵向薄。
        drawLayer(consumer, poseStack, width * pulse, height * pulse, 0.0F,
                0.90F * alphaScale, 190, 145, 255, frameV0, frameV1);
        // 内层高光刃芯。
        drawLayer(consumer, poseStack, width * 0.94F, height * 0.52F, 0.02F,
                0.95F * alphaScale, 246, 240, 255, frameV0, frameV1);

        poseStack.popPose();
    }

    private static void drawLayer(VertexConsumer consumer, PoseStack poseStack,
                                  float width, float height, float z,
                                  float alpha, int red, int green, int blue,
                                  float frameV0, float frameV1) {
        if (alpha <= 0.005F) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, z);
        poseStack.scale(width, height, 1.0F);

        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        Matrix4f matrix = poseStack.last().pose();

        // 面向 +Z 的四边形；只采样当前帧里横向刃形所在的条带。
        vertex(consumer, matrix, -0.5F, -0.5F, 0.0F, red, green, blue, a, U0, frameV1);
        vertex(consumer, matrix, 0.5F, -0.5F, 0.0F, red, green, blue, a, U1, frameV1);
        vertex(consumer, matrix, 0.5F, 0.5F, 0.0F, red, green, blue, a, U1, frameV0);
        vertex(consumer, matrix, -0.5F, 0.5F, 0.0F, red, green, blue, a, U0, frameV0);

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

