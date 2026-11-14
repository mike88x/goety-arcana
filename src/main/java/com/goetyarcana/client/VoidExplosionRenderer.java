package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.entity.VoidExplosionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 虚空爆炸的地面双环：前 25% 收缩，后 75% 扩散并淡出。
 */
public class VoidExplosionRenderer extends EntityRenderer<VoidExplosionEntity> {

    private static final ResourceLocation WHITE =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/item/void_bolt_white.png");

    public VoidExplosionRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidExplosionEntity entity) {
        return WHITE;
    }

    @Override
    public void render(VoidExplosionEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float progress = entity.getProgress();
        float radius;
        if (progress < 0.25F) {
            radius = 2.4F * (1.0F - progress / 0.25F);
        } else {
            radius = 3.0F * ((progress - 0.25F) / 0.75F);
        }
        if (radius <= 0.05F) {
            return;
        }
        float alpha = 1.0F - progress;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        pose.pushPose();
        // 抬到离地 0.3 格：贴脸爆炸时环不会被摄像机包在正中间，看起来像一圈巨方块。
        pose.translate(0.0D, 0.30D, 0.0D);
        ring(pose, consumer, radius, 0.70F, 0.92F, 1.0F, alpha);
        ring(pose, consumer, radius * 0.72F, 0.55F, 0.25F, 0.95F, alpha * 0.8F);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static void ring(PoseStack pose, VertexConsumer consumer, float radius,
                             float r, float g, float b, float alpha) {
        int segments = 32;
        Matrix4f m = pose.last().pose();
        for (int i = 0; i < segments; i++) {
            float a0 = i * Mth.TWO_PI / segments;
            float a1 = (i + 1) * Mth.TWO_PI / segments;
            float x0 = Mth.cos(a0) * radius;
            float z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            float inner = radius * 0.93F;
            float ix0 = Mth.cos(a0) * inner;
            float iz0 = Mth.sin(a0) * inner;
            float ix1 = Mth.cos(a1) * inner;
            float iz1 = Mth.sin(a1) * inner;
            vertex(consumer, m, x0, 0, z0, r, g, b, alpha);
            vertex(consumer, m, ix0, 0, iz0, r, g, b, alpha);
            vertex(consumer, m, ix1, 0, iz1, r, g, b, alpha);
            vertex(consumer, m, x1, 0, z1, r, g, b, alpha);
            vertex(consumer, m, x1, 0, z1, r, g, b, alpha);
            vertex(consumer, m, ix1, 0, iz1, r, g, b, alpha);
            vertex(consumer, m, ix0, 0, iz0, r, g, b, alpha);
            vertex(consumer, m, x0, 0, z0, r, g, b, alpha);
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f m,
                               float x, float y, float z, float r, float g, float b, float alpha) {
        consumer.vertex(m, x, y, z)
                .color(r, g, b, alpha)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
