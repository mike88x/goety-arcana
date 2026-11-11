package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.VoidScytheBalance;
import com.goetyarcana.item.VoidScytheItem;
import com.goetyarcana.registry.ModParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 手持虚空镰刀时的环绕特效。
 *
 * <p>旧版每 3 tick 才在固定角度放一颗灵魂火，看起来是一跳一跳的。
 * 现在改成每 tick 连续发射自制虚空火：</p>
 * <ul>
 *   <li>闲置：两道相位相反的粒子流绕玩家缓慢旋转，形成连续光带；</li>
 *   <li>蓄力：粒子从外圈螺旋收束到胸口，蓄力越高收束越紧；</li>
 *   <li>满蓄：额外脉冲环 + 细小的反向传送门粒子，强调能量溢出。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class VoidScytheClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.level() == null) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean holdingMain = mainHand.getItem() instanceof VoidScytheItem;
        boolean holdingOff = offHand.getItem() instanceof VoidScytheItem;
        if (!holdingMain && !holdingOff) {
            return;
        }

        boolean charging = player.isUsingItem()
                && player.getUseItem().getItem() instanceof VoidScytheItem;
        Level level = player.level();
        int tick = player.tickCount;

        if (!charging) {
            emitIdleOrbit(level, player, tick, holdingMain);
            return;
        }

        emitChargeSpiral(level, player, tick, chargeRatio(player));
    }

    /**
     * 第一人称的轻微手部动作：左键挥砍时让镰刀横向甩出，右键蓄力时向身体中线收，
     * 与第三人称的双臂动作呼应。幅度刻意做小，避免和原版挥击/使用动画打架。
     */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getItemStack().getItem() instanceof VoidScytheItem)) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();

        if (player.isUsingItem()
                && player.getUseItem().getItem() instanceof VoidScytheItem) {
            float charge = chargeRatio(player, event.getPartialTick());
            // 明显收向身体中线，配合第三人称的双手蓄力。
            poseStack.translate(-0.16F * charge, -0.09F * charge, -0.10F * charge);
            poseStack.mulPose(Axis.YP.rotationDegrees(22.0F * charge));
            poseStack.mulPose(Axis.XP.rotationDegrees(-14.0F * charge));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-12.0F * charge));
            return;
        }

        float swing = Math.max(event.getSwingProgress(),
                player.getAttackAnim(event.getPartialTick()));
        if (swing <= 0.01F) {
            return;
        }

        float arc = Mth.sin(Mth.clamp(swing, 0.0F, 1.0F) * Mth.PI);
        // 第一人称的横向甩镰；幅度比原版短剑挥击更大，但仍然绕着手臂根部旋转。
        poseStack.translate(-0.12F * arc, -0.06F * arc, -0.10F * arc);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-38.0F * arc));
        poseStack.mulPose(Axis.YP.rotationDegrees(22.0F * arc));
        poseStack.mulPose(Axis.XP.rotationDegrees(-12.0F * arc));
    }

    /**
     * 闲置光带：每 tick 两颗相隔 180° 的虚空火，连续旋转，不会出现跳帧断点。
     */
    private static void emitIdleOrbit(Level level, LocalPlayer player, int tick, boolean holdingMain) {
        // 主手/副手只影响相位，保持画面稳定。
        float phase = holdingMain ? 0.0F : Mth.PI;
        for (int i = 0; i < 2; i++) {
            double angle = tick * 0.085D + phase + i * Math.PI;
            double radius = 0.92D + 0.07D * Math.sin(tick * 0.055D + i * Math.PI);
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 0.86D
                    + 0.26D * Math.sin(angle * 2.0D + tick * 0.035D)
                    + (i == 0 ? 0.0D : 0.28D);

            // 沿圆周切线方向给一点速度，粒子会稍微拖出弧线而不是原地一个点。
            double vx = -Math.sin(angle) * 0.012D;
            double vz = Math.cos(angle) * 0.012D;
            level.addParticle(ModParticles.VOID_FLAME.get(), x, y, z, vx, 0.008D, vz);
        }

        // 每 5 tick 补一颗更高更远的淡粒子，增加纵向层次。
        if (tick % 5 == 0) {
            double angle = tick * 0.085D + phase + Math.PI * 0.5D;
            double radius = 1.05D + 0.10D * Math.sin(tick * 0.04D);
            level.addParticle(ModParticles.VOID_FLAME.get(),
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + 1.35D + 0.12D * Math.sin(tick * 0.08D),
                    player.getZ() + Math.sin(angle) * radius,
                    -Math.sin(angle) * 0.008D, 0.006D, Math.cos(angle) * 0.008D);
        }
    }

    /**
     * 蓄力：外圈螺旋向胸口收束；满蓄时再叠一层向外扩散的脉冲环。
     */
    private static void emitChargeSpiral(Level level, LocalPlayer player, int tick, float charge) {
        double angle = tick * 0.30D;
        // 蓄力越高，收束半径越小。
        double radius = 1.65D - 1.05D * charge + 0.12D * Math.sin(tick * 0.11D);
        double x = player.getX() + Math.cos(angle) * radius;
        double z = player.getZ() + Math.sin(angle) * radius;
        double y = player.getY() + 0.45D + (tick % 9) / 9.0D * 1.05D;
        double pull = 0.045D + 0.035D * charge;

        level.addParticle(ModParticles.VOID_FLAME.get(), x, y, z,
                (player.getX() - x) * pull,
                0.012D,
                (player.getZ() - z) * pull);

        // 第二道相位相反的螺旋，让收束看起来是旋转的双股，而不是一圈点。
        double angle2 = angle + Math.PI;
        double radius2 = radius * 0.92D;
        level.addParticle(ModParticles.VOID_FLAME.get(),
                player.getX() + Math.cos(angle2) * radius2,
                player.getY() + 1.05D - (tick % 9) / 9.0D * 0.55D,
                player.getZ() + Math.sin(angle2) * radius2,
                (player.getX() - (player.getX() + Math.cos(angle2) * radius2)) * pull,
                0.010D,
                (player.getZ() - (player.getZ() + Math.sin(angle2) * radius2)) * pull);

        if (charge >= 0.999F) {
            // 满蓄：每 4 tick 一圈向外扩散的脉冲，配合刀身发光模型。
            if (tick % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double pulseAngle = tick * 0.12D + i * (Math.PI * 2.0D / 8.0D);
                    double pulseRadius = 0.55D + 0.08D * Math.sin(tick * 0.3D + i);
                    double px = player.getX() + Math.cos(pulseAngle) * pulseRadius;
                    double pz = player.getZ() + Math.sin(pulseAngle) * pulseRadius;
                    double py = player.getY() + 1.05D + 0.20D * Math.sin(pulseAngle * 2.0D);
                    level.addParticle(ModParticles.VOID_FLAME.get(), px, py, pz,
                            Math.cos(pulseAngle) * 0.02D,
                            0.015D,
                            Math.sin(pulseAngle) * 0.02D);
                }
            }

            if (tick % 3 == 0) {
                double eyeY = player.getY() + Math.min(1.45D, player.getEyeHeight() * 0.85D);
                level.addParticle(ParticleTypes.REVERSE_PORTAL,
                        player.getX() + (level.random.nextDouble() - 0.5D) * 0.7D,
                        eyeY + (level.random.nextDouble() - 0.5D) * 0.45D,
                        player.getZ() + (level.random.nextDouble() - 0.5D) * 0.7D,
                        0.0D, 0.012D, 0.0D);
            }
        }
    }

    private static float chargeRatio(LocalPlayer player) {
        return chargeRatio(player, 0.0F);
    }

    private static float chargeRatio(LocalPlayer player, float partialTick) {
        int span = VoidScytheBalance.FULL_CHARGE_TICKS - VoidScytheBalance.MIN_CHARGE_TICKS;
        float used = player.getTicksUsingItem() + partialTick - VoidScytheBalance.MIN_CHARGE_TICKS;
        return Mth.clamp(used / span, 0.0F, 1.0F);
    }

    private VoidScytheClientEvents() {
    }
}
