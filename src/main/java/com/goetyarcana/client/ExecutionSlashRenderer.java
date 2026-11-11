package com.goetyarcana.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.entity.ExecutionSlashEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 处决动画：一把巨大的虚空镰刀在目标头顶转下，叠加月牙斩击和虚空火。
 */
public class ExecutionSlashRenderer extends EntityRenderer<ExecutionSlashEntity> {

    private static final ResourceLocation SCYTHE =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/item/void_scythe.png");
    private static final ResourceLocation SLASH =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/entity/scythe_wave.png");
    private static final int FULL_LIGHT = 0xF000F0;

    public ExecutionSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ExecutionSlashEntity entity) {
        return SCYTHE;
    }

    @Override
    public void render(ExecutionSlashEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float tick = entity.tickCount + partialTick;
        float progress = Mth.clamp(tick / (float) ExecutionSlashEntity.LIFETIME, 0.0F, 1.0F);
        float alpha = Math.min(1.0F, tick / 3.0F) * (1.0F - Mth.clamp((progress - 0.62F) / 0.38F, 0.0F, 1.0F));
        if (alpha <= 0.01F) {
            return;
        }

        float scale = entity.getScale();
        float swing = -65.0F + 100.0F * Mth.clamp(tick / 14.0F, 0.0F, 1.0F);

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(swing));

        VertexConsumer slash = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(SLASH));
        drawQuad(slash, poseStack.last().pose(), scale * 1.75F, scale * 1.55F,
                0.0F, 0.72F * alpha, 120, 65, 255);

        VertexConsumer scythe = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(SCYTHE));
        drawQuad(scythe, poseStack.last().pose(), scale * 1.45F, scale * 1.45F,
                0.06F, 0.95F * alpha, 255, 255, 255);

        poseStack.mulPose(Axis.ZP.rotationDegrees(-145.0F));
        drawQuad(slash, poseStack.last().pose(), scale * 1.25F, scale * 1.05F,
                0.02F, 0.42F * alpha, 180, 120, 255);
        poseStack.popPose();
    }

    private static void drawQuad(VertexConsumer consumer, Matrix4f matrix,
                                 float width, float height, float z,
                                 float alpha, int red, int green, int blue) {
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        float halfW = width * 0.5F;
        float halfH = height * 0.5F;
        // 面向相机的四边形；法线给 +Z。
        vertex(consumer, matrix, -halfW, -halfH, z, red, green, blue, a, 0.0F, 1.0F);
        vertex(consumer, matrix, halfW, -halfH, z, red, green, blue, a, 1.0F, 1.0F);
        vertex(consumer, matrix, halfW, halfH, z, red, green, blue, a, 1.0F, 0.0F);
        vertex(consumer, matrix, -halfW, halfH, z, red, green, blue, a, 0.0F, 0.0F);
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
