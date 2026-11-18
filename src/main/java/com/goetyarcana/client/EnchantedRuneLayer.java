package com.goetyarcana.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * 附魔虚空苦力怕的符文光层（一组）。
 *
 * <p>符文按组分成好几张图，每组有各自的相位：亮度/透明度按正弦上下浮动，
 * 于是同一时刻有的符文亮着、有的几乎看不见，看上去像有光在身上流动。</p>
 */
public class EnchantedRuneLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private final ResourceLocation texture;
    private final float phase;

    public EnchantedRuneLayer(RenderLayerParent<T, M> parent, ResourceLocation texture, float phase) {
        super(parent);
        this.texture = texture;
        this.phase = phase;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity,
                       float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        float wave = (Mth.sin(ageInTicks * 0.045F + this.phase) + 1.0F) * 0.5F;
        if (wave < 0.08F) {
            // 暗就彻底暗掉，一点光都不留
            return;
        }
        float fade = (float) Math.pow((wave - 0.08F) / 0.92F, 1.6F);
        float red = 0.20F + 0.70F * fade;
        float green = 0.60F + 0.40F * fade;
        float blue = 0.50F + 0.50F * fade;
        float alpha = 1.0F * fade;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(this.texture));
        this.getParentModel().renderToBuffer(pose, consumer, 0xF000F0, OverlayTexture.NO_OVERLAY,
                red, green, blue, alpha);
    }
}
