package com.goetyarcana.mixin;

import com.goetyarcana.VoidScytheBalance;
import com.goetyarcana.item.VoidScytheItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 虚空镰刀的第三人称动作。
 *
 * <p>这里只注入 {@link HumanoidModel#setupAnim}，并且 {@code @Shadow} 字段直接
 * 使用 Forge 生产环境的 SRG 名（{@code f_102808_} 等），同时设置
 * {@code remap = false}。这样不再依赖 Mixin 对字段的 refmap 重映射，也避免
 * 1.9.2 的“Non-private field cannot be aliased”问题。</p>
 *
 * <p>注入父类而不是 {@code PlayerModel}：玩家模型的 {@code setupAnim} 会先
 * 调用 {@code super.setupAnim(...)}，再复制袖子/裤子/外套；动作加在父类末尾，
 * 子类随后复制时覆盖层会自然跟随。</p>
 *
 * <p>字段对应关系：f_102808_ = head，f_102810_ = body，
 * f_102811_ = rightArm，f_102812_ = leftArm。</p>
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {

    @Shadow(remap = false)
    @Final
    public ModelPart f_102808_; // head

    @Shadow(remap = false)
    @Final
    public ModelPart f_102810_; // body

    @Shadow(remap = false)
    @Final
    public ModelPart f_102811_; // rightArm

    @Shadow(remap = false)
    @Final
    public ModelPart f_102812_; // leftArm

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void goetyarcana$applyVoidScythePose(LivingEntity entity, float limbSwing,
                                                 float limbSwingAmount, float ageInTicks,
                                                 float netHeadYaw, float headPitch,
                                                 CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean mainScythe = mainHand.getItem() instanceof VoidScytheItem;
        boolean offScythe = offHand.getItem() instanceof VoidScytheItem;
        if (!mainScythe && !offScythe) {
            return;
        }

        // 第一人称下 PlayerRenderer 也会调用 setupAnim 来摆手臂；
        // 这时换姿势会让手臂和手持物品错位，所以只处理第三人称。
        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.player
                && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        ModelPart head = this.f_102808_;
        ModelPart body = this.f_102810_;
        ModelPart rightArm = this.f_102811_;
        ModelPart leftArm = this.f_102812_;
        if (head == null || body == null || rightArm == null || leftArm == null) {
            return;
        }

        float partialTick = ageInTicks - entity.tickCount;

        if (player.isUsingItem()
                && player.getUseItem().getItem() instanceof VoidScytheItem) {
            this.goetyarcana$applyChargePose(player, partialTick, ageInTicks, mainScythe,
                    body, head, rightArm, leftArm);
            return;
        }

        float attack = Mth.clamp(player.getAttackAnim(partialTick), 0.0F, 1.0F);
        if (attack > 0.01F) {
            this.goetyarcana$applySwingPose(attack, mainScythe, body, head, rightArm, leftArm);
        }
    }

    /**
     * 双手蓄力：镰刀斜举到胸前，左手向内收，身体微微前倾。
     */
    private void goetyarcana$applyChargePose(Player player, float partialTick,
                                             float ageInTicks, boolean mainScythe,
                                             ModelPart body, ModelPart head,
                                             ModelPart rightArm, ModelPart leftArm) {
        int used = player.getTicksUsingItem();
        float charge = Mth.clamp(
                (used + partialTick - VoidScytheBalance.MIN_CHARGE_TICKS)
                        / (float) (VoidScytheBalance.FULL_CHARGE_TICKS
                        - VoidScytheBalance.MIN_CHARGE_TICKS),
                0.0F, 1.0F);

        // 起手 5 tick 内淡入，避免按下右键瞬间姿势突变。
        float weight = Mth.clamp((used + partialTick) / (float) VoidScytheBalance.MIN_CHARGE_TICKS,
                0.0F, 1.0F);
        weight = weight * weight * (3.0F - 2.0F * weight);

        float tremble = 0.018F * charge * Mth.sin(ageInTicks * 1.7F);

        // 主手/副手镜像：不做镜像时统一把副手当作“左手握镰”处理。
        float handedness = mainScythe ? 1.0F : -1.0F;

        // 右键蓄力：双臂抬到胸前，左臂向内夹住镰柄。
        rightArm.xRot += (-1.02F + tremble) * weight;
        rightArm.yRot += (0.30F * handedness) * weight;
        rightArm.zRot += (-0.30F * handedness) * weight;

        leftArm.xRot += (-1.16F - tremble) * weight;
        leftArm.yRot += (-0.58F * handedness) * weight;
        leftArm.zRot += (0.48F * handedness) * weight;

        body.xRot += 0.055F * weight;
        body.yRot += (-0.08F * handedness) * weight;
        head.xRot += -0.10F * weight;
        head.yRot += (0.06F * handedness) * weight;
    }

    /**
     * 左键挥砍：右臂从右后方水平扫到左前方，左臂跟随支撑，躯干同步扭转。
     */
    private void goetyarcana$applySwingPose(float attack, boolean mainScythe,
                                            ModelPart body, ModelPart head,
                                            ModelPart rightArm, ModelPart leftArm) {
        float progress = Mth.clamp(attack, 0.0F, 1.0F);
        float weight = Mth.clamp(attack * 1.35F, 0.0F, 1.0F);
        float arc = Mth.sin(progress * Mth.PI);

        float handedness = mainScythe ? 1.0F : -1.0F;

        // 镰刀是长柄武器：先抬起，再横向扫过，而不是原版短剑的斜劈。
        rightArm.xRot += (-0.55F - 0.62F * arc) * weight;
        rightArm.yRot += (-0.95F + 1.85F * progress) * weight * handedness;
        rightArm.zRot += (-0.30F + 0.75F * progress) * weight * handedness;

        // 左手不只是装饰：跟着右手一起走，形成双手挥镰的感觉。
        leftArm.xRot += (-0.30F - 0.50F * arc) * weight;
        leftArm.yRot += (0.82F - 1.55F * progress) * weight * handedness;
        leftArm.zRot += (0.30F - 0.68F * progress) * weight * handedness;

        // 上身扭转，头往回转一点，避免挥砍时脸跟着甩走。
        body.yRot += (0.42F - 0.90F * progress) * weight * handedness;
        head.yRot += (-0.22F + 0.46F * progress) * weight * handedness;
        body.xRot += 0.045F * arc * weight;
    }
}
