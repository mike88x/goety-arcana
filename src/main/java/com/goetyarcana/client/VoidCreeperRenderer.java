package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.VoidBlastBalance;
import com.goetyarcana.entity.VoidCreeper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * 自然生成的虚空苦力怕：同一套虚空皮肤，愤怒后引信期间会膨胀。
 */
public class VoidCreeperRenderer extends MobRenderer<VoidCreeper, CreeperModel<VoidCreeper>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/entity/void_creeper_servant.png");
    private static final ResourceLocation EYES =
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "textures/entity/void_creeper_servant_glow.png");

    public VoidCreeperRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER)), 0.5F);
        this.addLayer(new Eyes(this));
    }

    @Override
    public ResourceLocation getTextureLocation(VoidCreeper entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(VoidCreeper entity, PoseStack pose, float partialTick) {
        if (entity.isAngry()) {
            float progress = 1.0F - Math.max(0, entity.getFuse())
                    / (float) VoidBlastBalance.VOID_CREEPER_FUSE_TICKS;
            float swell = 0.12F + 0.25F * progress;
            pose.scale(1.0F + swell, 1.0F + swell, 1.0F + swell);
        }
    }

    private static final class Eyes extends EyesLayer<VoidCreeper, CreeperModel<VoidCreeper>> {

        private Eyes(VoidCreeperRenderer parent) {
            super(parent);
        }

        @Override
        public RenderType renderType() {
            return RenderType.eyes(EYES);
        }
    }
}
