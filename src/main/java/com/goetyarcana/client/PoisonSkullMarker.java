package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.registry.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 中了藤毒的敌人头顶飘一个绿色骷髅头，提醒「这家伙正在被毒」。
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PoisonSkullMarker {

    private static final ResourceLocation SKULL =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/entity/poison_skull.png");
    private static final int FULL_LIGHT = 0xF000F0;

    private PoisonSkullMarker() {
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void onRender(RenderLivingEvent.Post event) {
        LivingEntity entity = (LivingEntity) event.getEntity();
        if (!entity.hasEffect(ModEffects.VINE_POISON.get())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();

        pose.pushPose();
        pose.translate(0.0D, entity.getBbHeight() + 0.45D, 0.0D);
        pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.55F, 0.55F, 0.55F);

        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(SKULL));
        vertex(consumer, matrix, normal, -0.5F, -0.5F, 0.0F, 1.0F);
        vertex(consumer, matrix, normal, 0.5F, -0.5F, 1.0F, 1.0F);
        vertex(consumer, matrix, normal, 0.5F, 0.5F, 1.0F, 0.0F);
        vertex(consumer, matrix, normal, -0.5F, 0.5F, 0.0F, 0.0F);
        pose.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float u, float v) {
        consumer.vertex(matrix, x, y, 0.0F)
                .color(255, 255, 255, 220)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_LIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
