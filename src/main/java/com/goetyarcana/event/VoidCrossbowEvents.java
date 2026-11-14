package com.goetyarcana.event;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.registry.ModEffects;
import com.goetyarcana.registry.ModItems;
import com.goetyarcana.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.TickEvent;
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
        if (!(arrow.getOwner() instanceof Player player) || !holdingCrossbow(player)) {
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

    private static boolean holdingCrossbow(Player player) {
        return player.getMainHandItem().is(ModItems.VOID_CROSSBOW.get())
                || player.getOffhandItem().is(ModItems.VOID_CROSSBOW.get());
    }
}





