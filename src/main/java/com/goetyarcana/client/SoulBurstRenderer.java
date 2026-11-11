package com.goetyarcana.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.entity.SoulBurstEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 灵魂弹渲染：一颗翻滚的「虚空方块」。
 *
 * 视觉构成：
 *  1. 外圈柔光（soul_burst_glow）——给方块镶一层紫色辉光；
 *  2. 方块本体：4 个侧面用「暗紫岩面 + 发光裂纹」的贴图，上下两面用带虚空符文的贴图；
 *  3. 拖尾：沿飞行反方向复制 4 层逐渐缩小、变淡的同款方块。
 *
 * 注意：RenderType.entityTranslucent 是 NEW_ENTITY 顶点格式，
 * 每个顶点必须补齐 position / color / uv0 / uv1 / uv2 / normal，
 * 少了 normal 会在 BufferBuilder.endVertex() 抛 "Not filled all elements of the vertex"。
 */
public class SoulBurstRenderer extends EntityRenderer<SoulBurstEntity> {

    private static final ResourceLocation SIDE =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/entity/soul_burst_side.png");
    private static final ResourceLocation TOP =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/entity/soul_burst_top.png");
    private static final ResourceLocation GLOW =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/entity/soul_burst_glow.png");

    private static final int FULL_LIGHT = 0xF000F0;

    public SoulBurstRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SoulBurstEntity entity) {
        return SIDE;
    }

    @Override
    public void render(SoulBurstEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        VertexConsumer side = bufferSource.getBuffer(RenderType.entityTranslucent(SIDE));
        VertexConsumer top = bufferSource.getBuffer(RenderType.entityTranslucent(TOP));
        VertexConsumer glow = bufferSource.getBuffer(RenderType.entityTranslucent(GLOW));

        float time = entity.tickCount + partialTick;
        float spin = time * 9.0F;

        // 由实体的朝向还原飞行方向（实体构造时已把朝向设为速度方向，服务端会同步给客户端）
        double yaw = Math.toRadians(entity.getYRot());
        double pitch = Math.toRadians(entity.getXRot());
        double dirX = -Math.sin(yaw) * Math.cos(pitch);
        double dirY = -Math.sin(pitch);
        double dirZ = Math.cos(yaw) * Math.cos(pitch);

        // 1) 外圈辉光
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        float halo = 2.0F + 0.15F * Mth.sin(time * 0.7F);
        poseStack.scale(halo, halo, halo);
        quad(glow, poseStack.last().pose(), 255, 255, 255, 255);
        poseStack.popPose();

        // 2) 拖尾：沿飞行反方向铺 4 层残影方块
        for (int i = 1; i <= 4; i++) {
            double back = 0.34D * i;
            poseStack.pushPose();
            poseStack.translate(-dirX * back, -dirY * back, -dirZ * back);
            float scale = 0.62F * (1.0F - 0.16F * i);
            poseStack.scale(scale, scale, scale);
            poseStack.mulPose(Axis.YP.rotationDegrees(spin - i * 16.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(spin * 0.7F + i * 12.0F));
            cube(side, top, poseStack.last().pose(), Math.max(18, 110 - i * 25));
            poseStack.popPose();
        }

        // 3) 本体
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.XP.rotationDegrees(spin * 0.7F));
        float pulse = 1.0F + 0.05F * Mth.sin(time * 0.9F);
        float size = 0.62F * pulse;
        poseStack.scale(size, size, size);
        cube(side, top, poseStack.last().pose(), 255);
        poseStack.popPose();
    }

    /**
     * 画一个立方体：4 个侧面用 side 贴图，上下两面用 top 贴图。
     */
    private static void cube(VertexConsumer side, VertexConsumer top, Matrix4f m, int alpha) {
        // +Z / -Z / +X / -X 四个侧面
        face(side, m, alpha, 0.0F, 0.0F, 1.0F,
                -0.5F, -0.5F, 0.5F, 0.5F, -0.5F, 0.5F, 0.5F, 0.5F, 0.5F, -0.5F, 0.5F, 0.5F);
        face(side, m, alpha, 0.0F, 0.0F, -1.0F,
                0.5F, -0.5F, -0.5F, -0.5F, -0.5F, -0.5F, -0.5F, 0.5F, -0.5F, 0.5F, 0.5F, -0.5F);
        face(side, m, alpha, 1.0F, 0.0F, 0.0F,
                0.5F, -0.5F, 0.5F, 0.5F, -0.5F, -0.5F, 0.5F, 0.5F, -0.5F, 0.5F, 0.5F, 0.5F);
        face(side, m, alpha, -1.0F, 0.0F, 0.0F,
                -0.5F, -0.5F, -0.5F, -0.5F, -0.5F, 0.5F, -0.5F, 0.5F, 0.5F, -0.5F, 0.5F, -0.5F);
        // +Y / -Y 上下两面
        face(top, m, alpha, 0.0F, 1.0F, 0.0F,
                -0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, -0.5F, -0.5F, 0.5F, -0.5F);
        face(top, m, alpha, 0.0F, -1.0F, 0.0F,
                -0.5F, -0.5F, -0.5F, 0.5F, -0.5F, -0.5F, 0.5F, -0.5F, 0.5F, -0.5F, -0.5F, 0.5F);
    }

    private static void face(VertexConsumer consumer, Matrix4f matrix, int alpha,
                             float nx, float ny, float nz,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        consumer.vertex(matrix, x0, y0, z0).color(255, 255, 255, alpha)
                .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_LIGHT)
                .normal(nx, ny, nz).endVertex();
        consumer.vertex(matrix, x1, y1, z1).color(255, 255, 255, alpha)
                .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_LIGHT)
                .normal(nx, ny, nz).endVertex();
        consumer.vertex(matrix, x2, y2, z2).color(255, 255, 255, alpha)
                .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_LIGHT)
                .normal(nx, ny, nz).endVertex();
        consumer.vertex(matrix, x3, y3, z3).color(255, 255, 255, alpha)
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_LIGHT)
                .normal(nx, ny, nz).endVertex();
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, int r, int g, int b, int a) {
        consumer.vertex(matrix, -0.5F, -0.5F, 0.0F).color(r, g, b, a)
                .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_LIGHT)
                .normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, 0.5F, -0.5F, 0.0F).color(r, g, b, a)
                .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_LIGHT)
                .normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, 0.5F, 0.5F, 0.0F).color(r, g, b, a)
                .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_LIGHT)
                .normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, -0.5F, 0.5F, 0.0F).color(r, g, b, a)
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_LIGHT)
                .normal(0.0F, 0.0F, 1.0F).endVertex();
    }
}
