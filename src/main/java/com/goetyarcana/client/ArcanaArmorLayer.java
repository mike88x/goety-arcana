package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.item.ArcanaArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * 给玩家额外画出盔甲的冠饰 / 肩甲 / 裙甲 / 护膝。
 *
 * <p>只画玩家穿着的对应部位，贴图按套装（虚空 / 灵魂）取。</p>
 */
public class ArcanaArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    /** 额外的盒子几何先关掉：目前效果不如纯贴图，等有正式模型再打开。 */
    private static final boolean ENABLE_EXTRAS = false;

    private static ModelPart extras;

    public ArcanaArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    private static ModelPart extras() {
        if (extras == null) {
            extras = Minecraft.getInstance().getEntityModels()
                    .bakeLayer(ArcanaArmorExtrasModel.LAYER_LOCATION);
        }
        return extras;
    }

    private static String setOf(LivingEntity player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.getItem() instanceof ArcanaArmorItem armor) {
            return armor.armorSet();
        }
        return null;
    }

    private static ResourceLocation texture(String set) {
        return new ResourceLocation(GoetyArcanaMod.MOD_ID,
                "textures/models/armor/" + set + "_extras.png");
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T player,
                       float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!ENABLE_EXTRAS || extras() == null) {
            return;
        }
        ModelPart root = extras();
        HumanoidModel<T> parent = this.getParentModel();

        String helmet = setOf(player, EquipmentSlot.HEAD);
        boolean voidSet = "void".equals(helmet);
        if (helmet != null) {
            var consumer = buffers.getBuffer(RenderType.armorCutoutNoCull(texture(helmet)));
            pose.pushPose();
            parent.head.translateAndRotate(pose);
            if (voidSet) {
                root.getChild("crest").render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
                root.getChild("horn_left").render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
                root.getChild("horn_right").render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
            } else {
                root.getChild("soul_hood").render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
            }
            pose.popPose();
        }

        String chest = setOf(player, EquipmentSlot.CHEST);
        if (chest != null) {
            var consumer = buffers.getBuffer(RenderType.armorCutoutNoCull(texture(chest)));
            pose.pushPose();
            parent.body.translateAndRotate(pose);
            boolean chestVoid = "void".equals(chest);
            if (chestVoid) {
                root.getChild("pauldron_left_cap").render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
                root.getChild("pauldron_left").render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
                root.getChild("pauldron_right_cap").render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
                root.getChild("pauldron_right").render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
                root.getChild("skirt_front").render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
            } else {
                root.getChild("soul_pad_left").render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
                root.getChild("soul_pad_right").render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
                root.getChild("soul_cloth").render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
            }
            pose.popPose();
        }

        String legs = setOf(player, EquipmentSlot.LEGS);
        if (legs != null) {
            var consumer = buffers.getBuffer(RenderType.armorCutoutNoCull(texture(legs)));
            pose.pushPose();
            parent.body.translateAndRotate(pose);
            if ("void".equals(legs)) {
                root.getChild("knee_left").render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
                root.getChild("knee_right").render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
            }
            pose.popPose();
        }
    }
}
