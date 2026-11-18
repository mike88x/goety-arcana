package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.entity.BrambleSeedEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 种子：用小方块拼的立体种子模型——下尖、中宽、上收，再顶一小截绿茎。
 *
 * <p>贴图 32x32：左上 16x16 是种子主体（不透明，四面共用），右上 8x8 是茎。</p>
 */
public class BrambleSeedRenderer extends EntityRenderer<BrambleSeedEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/entity/bramble_seed.png");
    private static final int FULL_LIGHT = 0xF000F0;

    /** 主体与茎在 32x32 贴图里的 UV 区域。 */
    private static final float[] BODY_UV = {0.0F, 0.0F, 0.5F, 0.5F};
    private static final float[] STEM_UV = {0.5F, 0.0F, 0.75F, 0.25F};

    public BrambleSeedRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BrambleSeedEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BrambleSeedEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        if (!entity.isStuck()) {
            pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 16.0F));
        } else {
            pose.mulPose(Axis.YP.rotationDegrees(entity.getYRot() * 0.6F));
        }
        pose.scale(0.72F, 0.72F, 0.72F);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        // 下尖 → 中宽 → 上收，最后是茎
        box(consumer, pose.last(), -0.05F, -0.20F, -0.05F, 0.05F, -0.12F, 0.05F, BODY_UV);
        box(consumer, pose.last(), -0.10F, -0.13F, -0.10F, 0.10F, -0.01F, 0.10F, BODY_UV);
        box(consumer, pose.last(), -0.07F, -0.02F, -0.07F, 0.07F, 0.10F, 0.07F, BODY_UV);
        box(consumer, pose.last(), -0.02F, 0.10F, -0.02F, 0.02F, 0.18F, 0.02F, STEM_UV);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static void box(VertexConsumer consumer, PoseStack.Pose entry,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float[] uv) {
        Matrix4f m = entry.pose();
        Matrix3f n = entry.normal();
        float u0 = uv[0];
        float v0 = uv[1];
        float u1 = uv[2];
        float v1 = uv[3];
        // +Z
        face(consumer, m, n, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, u0, v0, u1, v1);
        // -Z（镜像，避免字面反过来）
        face(consumer, m, n, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, u1, v0, u0, v1);
        // +X
        face(consumer, m, n, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, u0, v0, u1, v1);
        // -X
        face(consumer, m, n, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, u0, v0, u1, v1);
        // +Y / -Y
        face(consumer, m, n, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, u0, v0, u1, v1);
        face(consumer, m, n, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, u0, v0, u1, v1);
    }

    private static void face(VertexConsumer consumer, Matrix4f m, Matrix3f n,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float nx, float ny, float nz,
                             float u0, float v0, float u1, float v1) {
        vertex(consumer, m, n, x0, y0, z0, u0, v0, nx, ny, nz);
        vertex(consumer, m, n, x1, y1, z1, u1, v0, nx, ny, nz);
        vertex(consumer, m, n, x2, y2, z2, u1, v1, nx, ny, nz);
        vertex(consumer, m, n, x3, y3, z3, u0, v1, nx, ny, nz);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f m, Matrix3f n,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        consumer.vertex(m, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_LIGHT)
                .normal(n, nx, ny, nz)
                .endVertex();
    }
}
