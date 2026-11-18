package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.entity.BrambleVinesEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 缠绕藤蔓：**一根**藤从地下钻出来，螺旋绕住目标，然后收紧。
 *
 * <ul>
 *   <li>前 {@value #GROW_TICKS} tick 从土里往上长；</li>
 *   <li>接着 {@value #TIGHTEN_TICKS} tick 收紧（半径缩到 75%、整体略微压低），之后保持一点点挤压脉动；</li>
 *   <li>整根藤随时间缓慢旋转，看起来像在往身上缠；</li>
 *   <li>半径、高度、藤条粗细都按目标体型缩放：坚守者会得到明显更大的藤蔓，鸡之类就是小藤。</li>
 * </ul>
 */
public class BrambleVinesRenderer extends EntityRenderer<BrambleVinesEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/entity/bramble_strand.png");

    private static final int SEGMENTS = 48;
    private static final float TURNS = 2.0F;
    private static final float UV_REPEAT = 3.5F;
    private static final int GROW_TICKS = 16;
    private static final int TIGHTEN_TICKS = 20;

    public BrambleVinesRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BrambleVinesEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BrambleVinesEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        float life = entity.getLife() + partialTick;
        float grow = Mth.clamp(age / GROW_TICKS, 0.0F, 1.0F);
        float tighten = Mth.clamp((age - GROW_TICKS) / TIGHTEN_TICKS, 0.0F, 1.0F);
        // 最后 DEATH_TICKS：藤蔓从尖端缩回地下，同时变细
        float deathStart = entity.getEntangleTicks()
                - com.goetyarcana.entity.BrambleVinesEntity.DEATH_TICKS;
        float death = Mth.clamp((life - deathStart)
                / (float) com.goetyarcana.entity.BrambleVinesEntity.DEATH_TICKS, 0.0F, 1.0F);

        // 按目标体型缩放（僵尸 ≈ 1.0），半径至少比身体半宽多一点，避免穿模
        float width = Math.max(0.25F, entity.getTargetWidth());
        float height = Math.max(0.5F, entity.getTargetHeight());
        float halfWidth = width * 0.5F;
        float radius = Math.max(halfWidth + 0.24F, height * 0.17F + 0.22F);
        radius = Mth.clamp(radius, 0.45F, 1.9F);
        float vineHeight = Mth.clamp(height * 0.92F, 0.8F, 4.6F);
        float thickness = Mth.clamp(radius * 0.45F, 0.16F, 0.52F);

        // 收紧 + 之后的轻微挤压脉动
        float squeeze = 1.0F - 0.2F * tighten;
        radius *= squeeze;
        vineHeight *= 1.0F - 0.04F * tighten;
        if (tighten >= 1.0F && death <= 0.0F) {
            radius += Mth.sin(age * 0.35F) * 0.012F;
        }
        // 死亡：整体缩回去并变细
        radius *= 1.0F - 0.45F * death;
        thickness *= 1.0F - 0.5F * death;
        vineHeight *= 1.0F - 0.15F * death;

        float spin = age * 0.05F;
        int drawn = Math.max(1, Math.round(grow * (1.0F - death) * SEGMENTS));
        if (death >= 1.0F) {
            return;
        }

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose entry = pose.last();
        Matrix4f matrix = entry.pose();
        Matrix3f normal = entry.normal();

        double[] previous = helix(0.0D, radius, vineHeight, spin);
        for (int i = 0; i < drawn; i++) {
            double t = (i + 1) / (double) SEGMENTS;
            double[] current = helix(t, radius, vineHeight, spin);
            ribbon(consumer, matrix, normal, previous, current, thickness,
                    (float) i / SEGMENTS, (float) (i + 1) / SEGMENTS, light);
            previous = current;
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** 螺旋线上的点；y 从地下 -0.15 开始往上。 */
    private static double[] helix(double t, float radius, float height, float spin) {
        double angle = t * (Math.PI * 2.0D) * TURNS - Math.PI / 2.0D + spin;
        double r = radius * (1.0D - 0.24D * t);
        double wobble = Math.sin(spin * 2.4D + t * 4.6D) * 0.05D;
        double x = Math.cos(angle) * r + wobble;
        double z = Math.sin(angle) * r;
        double y = -0.15D + t * (height + 0.15D);
        return new double[]{x, y, z};
    }

    /** 两片交叉的带子，给藤条一点体积感。 */
    private static void ribbon(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               double[] from, double[] to, float halfWidth,
                               float v0, float v1, int light) {
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double dz = to[2] - from[2];
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-5D) {
            return;
        }
        dx /= length;
        dy /= length;
        dz /= length;

        // 第一片：水平方向的宽度
        double sx = -dz;
        double sz = dx;
        double sl = Math.sqrt(sx * sx + sz * sz);
        if (sl < 1.0E-4D) {
            sx = 1.0D;
            sz = 0.0D;
            sl = 1.0D;
        }
        sx = sx / sl * halfWidth;
        sz = sz / sl * halfWidth;
        quad(consumer, matrix, normal, from, to, sx, 0.0D, sz, v0, v1, light);

        // 第二片：绕 90° 的另一条宽度方向
        double tx = dy * sz;
        double ty = dz * sx - dx * sz;
        double tz = -dy * sx;
        double tl = Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (tl > 1.0E-4D) {
            tx = tx / tl * halfWidth;
            ty = ty / tl * halfWidth;
            tz = tz / tl * halfWidth;
            quad(consumer, matrix, normal, from, to, tx, ty, tz, v0, v1, light);
        }
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                             double[] from, double[] to,
                             double ox, double oy, double oz,
                             float v0, float v1, int light) {
        float u0 = 0.0F;
        float u1 = 1.0F;
        float va = v0 * UV_REPEAT;
        float vb = v1 * UV_REPEAT;
        consumer.vertex(matrix, (float) (from[0] - ox), (float) (from[1] - oy), (float) (from[2] - oz))
                .color(255, 255, 255, 255).uv(u0, va)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, (float) (to[0] - ox), (float) (to[1] - oy), (float) (to[2] - oz))
                .color(255, 255, 255, 255).uv(u0, vb)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, (float) (to[0] + ox), (float) (to[1] + oy), (float) (to[2] + oz))
                .color(255, 255, 255, 255).uv(u1, vb)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, (float) (from[0] + ox), (float) (from[1] + oy), (float) (from[2] + oz))
                .color(255, 255, 255, 255).uv(u1, va)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }
}
