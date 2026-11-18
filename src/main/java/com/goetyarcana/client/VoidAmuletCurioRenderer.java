package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * 虚空护身符挂在胸口的渲染：一片薄方片，跟着走路/呼吸轻轻摆动，符文亮度随时间呼吸。
 *
 * <p>Curios 传进来的 PoseStack 只做过 push/pop，坐标系是实体渲染空间（原点在脚底，Y 向上，
 * 模型正面朝 -Z），所以胸前位置是 y≈1.15、z≈-0.17。</p>
 */
public class VoidAmuletCurioRenderer implements ICurioRenderer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/item/void_amulet.png");

    /** 校准探针：确认位置后关掉。 */
    private static final boolean PROBE = false;

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext slotContext, PoseStack pose,
            RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light,
            float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {

        LivingEntity entity = slotContext.entity();
        pose.pushPose();
        ICurioRenderer.translateIfSneaking(pose, entity);
        // 直接锚到身体模型：先把坐标系移到躯干中心，再往前推一点点
        if (parent.getModel() instanceof net.minecraft.client.model.HumanoidModel<?> humanoid) {
            humanoid.body.translateAndRotate(pose);
        }
        // 在 body 模型空间里，前方是 +Z
        // 探针结论：+Z 那一面在背后，所以正面是 -Z
        // 这个坐标系里 Y 是反的（数值越小越靠上），所以要往下就用正值
        pose.translate(0.0D, 0.16D, -0.20D);
        pose.scale(0.36F, 0.36F, 0.36F);

        // 走路摆动 + 呼吸起伏
        float sway = Mth.sin(ageInTicks * 0.13F) * 0.10F + limbSwingAmount * 0.06F;
        // 方片的 V 方向是反的：先转 180° 让牙朝下、红绳在上
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F + sway * 22.0F));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(8.0F));
        drawQuad(pose, buffers, light, ageInTicks, 1.0F, 1.0F, 1.0F);

        pose.popPose();
    }

    private void drawQuad(PoseStack pose, MultiBufferSource buffers, int light,
                          float ageInTicks, float cr, float cg, float cb) {
        pose.pushPose();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float half = 0.5F;
        float glow = 0.72F + 0.28F * Mth.sin(ageInTicks * 0.09F);
        float r = cr * glow;
        float g = cg * glow;
        float b = cb;
        // 正面（-Z）
        vertex(consumer, m, n, -half, -half, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F, light, r, g, b);
        vertex(consumer, m, n, half, -half, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, -1.0F, light, r, g, b);
        vertex(consumer, m, n, half, half, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F, light, r, g, b);
        vertex(consumer, m, n, -half, half, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, light, r, g, b);
        // 背面（+Z）
        vertex(consumer, m, n, half, half, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, light, r, g, b);
        vertex(consumer, m, n, -half, half, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, light, r, g, b);
        vertex(consumer, m, n, -half, -half, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, light, r, g, b);
        vertex(consumer, m, n, half, -half, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, light, r, g, b);
        pose.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f m, Matrix3f n,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, int light,
                               float r, float g, float b) {
        consumer.vertex(m, x, y, z)
                .color(r, g, b, 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(n, nx, ny, nz)
                .endVertex();
    }
}
