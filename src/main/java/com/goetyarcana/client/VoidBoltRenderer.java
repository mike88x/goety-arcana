package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.entity.VoidBoltEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class VoidBoltRenderer extends ArrowRenderer<VoidBoltEntity> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            GoetyArcanaMod.MOD_ID, "textures/entity/projectiles/void_bolt.png");

    public VoidBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidBoltEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(VoidBoltEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        if (entity.isInGround()) {
            float t = entity.tickCount + partialTick;
            pose.translate(Mth.sin(t * 0.9F) * 0.02F, Mth.sin(t * 1.3F) * 0.015F, 0.0F);
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
        pose.popPose();
    }
}
