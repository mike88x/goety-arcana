package com.goetyarcana.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * 两套盔甲的额外部件几何：冠饰、小角、肩甲、裙甲、护膝。
 *
 * <p>注意这里**不继承 HumanoidModel**——`HumanoidModel` 的构造函数要求
 * `head/body/right_arm/...` 这些原版零件，我们只想要几个额外的盒子，
 * 所以直接 bake 出一个 {@link ModelPart} 根节点自行取子节点。</p>
 */
public final class ArcanaArmorExtrasModel {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(com.goetyarcana.GoetyArcanaMod.MOD_ID, "arcana_armor_extras"), "main");

    public static final LayerDefinition LAYER = createLayer();

    private ArcanaArmorExtrasModel() {
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 头盔冠饰 + 两侧小角
        root.addOrReplaceChild("crest",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -2.0F, -1.0F, 8.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, -7.0F, -4.0F));
        root.addOrReplaceChild("horn_left",
                CubeListBuilder.create().texOffs(0, 3).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(4.5F, -7.0F, -1.0F));
        root.addOrReplaceChild("horn_right",
                CubeListBuilder.create().texOffs(8, 3).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(-4.5F, -7.0F, -1.0F));

        // 肩甲：上盖 + 下沿两段，做出台阶感
        root.addOrReplaceChild("pauldron_left_cap",
                CubeListBuilder.create().texOffs(0, 8).addBox(-0.5F, -1.0F, -2.5F, 3.5F, 2.0F, 5.0F),
                PartPose.offset(4.0F, -6.5F, 0.0F));
        root.addOrReplaceChild("pauldron_left",
                CubeListBuilder.create().texOffs(0, 12).addBox(-0.5F, -0.5F, -2.5F, 3.5F, 3.0F, 5.0F),
                PartPose.offset(4.0F, -5.0F, 0.0F));
        root.addOrReplaceChild("pauldron_right_cap",
                CubeListBuilder.create().texOffs(16, 8).addBox(-3.0F, -1.0F, -2.5F, 3.5F, 2.0F, 5.0F),
                PartPose.offset(-4.0F, -6.5F, 0.0F));
        root.addOrReplaceChild("pauldron_right",
                CubeListBuilder.create().texOffs(16, 12).addBox(-3.0F, -0.5F, -2.5F, 3.5F, 3.0F, 5.0F),
                PartPose.offset(-4.0F, -5.0F, 0.0F));

        // 裙甲：三片
        root.addOrReplaceChild("skirt_front",
                CubeListBuilder.create().texOffs(0, 20).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 8.5F, 0.0F));
        root.addOrReplaceChild("skirt_left",
                CubeListBuilder.create().texOffs(0, 24).addBox(2.5F, 0.0F, -2.5F, 2.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 8.5F, 0.0F));
        root.addOrReplaceChild("skirt_right",
                CubeListBuilder.create().texOffs(12, 24).addBox(-4.5F, 0.0F, -2.5F, 2.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 8.5F, 0.0F));

        // 护膝
        root.addOrReplaceChild("knee_left",
                CubeListBuilder.create().texOffs(24, 20).addBox(-1.5F, -1.0F, -2.5F, 3.0F, 2.0F, 1.0F),
                PartPose.offset(2.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("knee_right",
                CubeListBuilder.create().texOffs(24, 23).addBox(-1.5F, -1.0F, -2.5F, 3.0F, 2.0F, 1.0F),
                PartPose.offset(-2.0F, 14.0F, 0.0F));

        // ---- 灵魂套专属：兜帽环 + 长布披 + 小肩垫 ----
        root.addOrReplaceChild("soul_hood",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -1.5F, -4.5F, 9.0F, 2.0F, 9.0F),
                PartPose.offset(0.0F, -7.5F, 0.0F));
        root.addOrReplaceChild("soul_cloth",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 7.0F, 4.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("soul_pad_left",
                CubeListBuilder.create().texOffs(20, 8).addBox(-0.5F, -1.0F, -2.5F, 3.0F, 2.0F, 5.0F),
                PartPose.offset(4.0F, -5.0F, 0.0F));
        root.addOrReplaceChild("soul_pad_right",
                CubeListBuilder.create().texOffs(20, 12).addBox(-2.5F, -1.0F, -2.5F, 3.0F, 2.0F, 5.0F),
                PartPose.offset(-4.0F, -5.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    /** 便捷取子部件。 */
    public static ModelPart part(ModelPart root, String name) {
        return root.getChild(name);
    }
}
