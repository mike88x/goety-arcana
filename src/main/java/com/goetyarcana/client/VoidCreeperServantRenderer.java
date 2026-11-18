package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.entity.VoidCreeperServant;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 虚空苦力怕仆从的渲染：苦力怕模型 + 虚空皮肤，眼睛用自发光层。
 */
public class VoidCreeperServantRenderer
        extends MobRenderer<VoidCreeperServant, CreeperModel<VoidCreeperServant>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/entity/void_creeper_servant.png");
    private static final ResourceLocation EYES =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/entity/void_creeper_servant_glow.png");

    public VoidCreeperServantRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER)), 0.5F);
        this.addLayer(new VoidEyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(VoidCreeperServant entity) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(VoidCreeperServant entity, com.mojang.blaze3d.vertex.PoseStack pose,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, pose, ageInTicks, rotationYaw, partialTicks);
        if (entity.getServantState() == 0) {
            // 待机：轻微起伏 + 左右摇摆，别像根木头桩子
            float breath = (float) Math.sin(ageInTicks * 0.09F);
            float sway = (float) Math.sin(ageInTicks * 0.035F);
            pose.translate(0.0D, breath * 0.025D, 0.0D);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(sway * 3.5F));
            pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(sway * 1.2F));
        }
    }

    @Override
    protected void scale(VoidCreeperServant entity, com.mojang.blaze3d.vertex.PoseStack pose,
                         float partialTick) {
        int state = entity.getServantState();
        if (state == 2) {
            float progress = 1.0F - Math.max(0, entity.getFuseLeft())
                    / (float) com.goetyarcana.VoidBlastBalance.FUSE_TICKS;
            float swell = 0.12F + 0.22F * progress;
            pose.scale(1.0F + swell, 1.0F + swell, 1.0F + swell);
        } else if (state == 1) {
            pose.scale(1.03F, 1.03F, 1.03F);
        }
    }

    private static final class VoidEyesLayer
            extends EyesLayer<VoidCreeperServant, CreeperModel<VoidCreeperServant>> {

        private VoidEyesLayer(VoidCreeperServantRenderer parent) {
            super(parent);
        }

        @Override
        public RenderType renderType() {
            return RenderType.eyes(EYES);
        }
    }
}
