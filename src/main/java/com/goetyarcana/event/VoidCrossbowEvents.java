package com.goetyarcana.event;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.registry.ModEffects;
import com.goetyarcana.registry.ModItems;
import com.goetyarcana.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 虚空之眼弩的命中叠层、换武器清除，以及带层数时的头顶紫火。
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VoidCrossbowEvents {

    private static final int DURATION = 20 * 20;

    private VoidCrossbowEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target instanceof Player) {
            return;
        }
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getOwner() instanceof Player player)) {
            return;
        }
        if (arrow instanceof com.goetyarcana.entity.VoidBoltEntity) {
            // 弩矢自己会在命中时叠层（末影人吃的是无 owner 的魔法伤害，事件里拿不到 directEntity）
            return;
        }
        if (!holdingCrossbow(player)) {
            return;
        }

        addEyeStack(player, target);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        Player player = event.player;
        MobEffectInstance eye = player.getEffect(ModEffects.VOID_EYE.get());
        if (eye == null) {
            return;
        }
        if (!holdingCrossbow(player)) {
            player.removeEffect(ModEffects.VOID_EYE.get());
            return;
        }
        crown(player, eye);
    }

    /**
     * 头顶紫火：拿弩且带虚空之眼时每 4 tick 冒一小簇。
     *
     * <p>层数越高越明显：1/2/3 层分别冒 1/2/3 颗，散得更开、上升更快，
     * 连起来就是从“一小缕”到“一圈火冠”的差别。</p>
     */
    private static void crown(Player player, MobEffectInstance eye) {
        if (player.tickCount % 4 != 0 || !(player.level() instanceof ServerLevel server)) {
            return;
        }
        int stacks = Math.min(3, eye.getAmplifier() + 1);
        double top = player.getY() + player.getBbHeight() + 0.08D;
        double spread = 0.10D + 0.05D * stacks;
        server.sendParticles(ModParticles.VOID_FLAME.get(),
                player.getX(), top, player.getZ(),
                stacks, spread, 0.02D, spread,
                0.008D + 0.005D * stacks);
    }

    /**
     * 流血的表现：每隔几 tick 从身上滴下红色粉尘，每秒再蹦两个伤害指示粒子。
     *
     * <p>效果本身由 {@code BleedEffect} 结算，这里只管视觉；只在服务端跑，
     * 通过 {@code sendParticles} 同步给附近玩家。</p>
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !entity.hasEffect(ModEffects.BLEED.get())) {
            return;
        }
        if (entity.tickCount % 4 != 0 || !(entity.level() instanceof ServerLevel server)) {
            return;
        }
        double x = entity.getX();
        double z = entity.getZ();
        double y = entity.getY() + entity.getBbHeight()
                * (0.55D + 0.35D * entity.getRandom().nextDouble());
        // 血滴自己会往下掉（有重力、会落地），所以只在上半身生成
        server.sendParticles(ModParticles.BLOOD_DRIP.get(),
                x, y, z, 1, 0.18D, 0.12D, 0.18D, 0.01D);
        if (entity.getRandom().nextInt(3) == 0) {
            server.sendParticles(ModParticles.BLOOD_DRIP.get(),
                    x, y, z, 1, 0.22D, 0.18D, 0.22D, 0.02D);
        }
        if (entity.tickCount % 20 == 0) {
            server.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    x, y, z, 1, 0.12D, 0.12D, 0.12D, 0.05D);
        }
    }

    /** 给玩家叠一层虚空之眼；三层时额外给目标流血。弩矢与普通箭都会调到这里。 */
    public static void addEyeStack(Player player, LivingEntity target) {
        // 规则回到「必须手上有虚空弩」：切手/换武器命中不再叠层（层数也会在 tick 里被清掉）
        if (!holdingCrossbow(player)) {
            return;
        }
        MobEffectInstance current = player.getEffect(ModEffects.VOID_EYE.get());
        int amplifier = current == null ? 0 : Math.min(2, current.getAmplifier() + 1);
        player.addEffect(new MobEffectInstance(ModEffects.VOID_EYE.get(), DURATION, amplifier,
                false, true, true));
        if (amplifier >= 2) {
            target.addEffect(new MobEffectInstance(ModEffects.BLEED.get(), 120, 0,
                    false, true, true));
        }
    }

    public static boolean holdingCrossbow(Player player) {
        return player.getMainHandItem().is(ModItems.VOID_CROSSBOW.get())
                || player.getOffhandItem().is(ModItems.VOID_CROSSBOW.get());
    }
}





