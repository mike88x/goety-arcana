package com.goetyarcana.event;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「缠绕」的硬控：不能动、不能攻击、不能用物品和技能。
 *
 * <p>位置冻结与 {@code noAi} 的保存/还原由 {@code BrambleVinesEntity} 负责
 * （它知道目标、也活得到结束）；这里只做每 tick 的清速度、停寻路、打断使用，
 * 以及取消使用物品 / 右键交互 / 攻击。BOSS 与精英没有任何豁免。</p>
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BrambleBindEvents {

    private BrambleBindEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(ModEffects.ENTANGLED.get())) {
            return;
        }
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        entity.setSprinting(false);
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        if (entity instanceof Player player && player.isUsingItem()) {
            player.stopUsingItem();
        }
    }

    /** 藤毒期间禁止任何治疗：自然回血、药水、法术、Boss 自愈都挡住。 */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        var entity = event.getEntity();
        if (entity.hasEffect(ModEffects.VINE_POISON.get())) {
            event.setCanceled(true);
            return;
        }
        if (entity.level() instanceof net.minecraft.server.level.ServerLevel server
                && com.goetyarcana.combat.VinePoisonTracker.isPoisoned(entity, (int) server.getGameTime())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker
                && attacker.hasEffect(ModEffects.ENTANGLED.get())) {
            event.setCanceled(true);
        }
    }

    /** 扎根期间禁止瞬移：末影人、末影类 BOSS 的传送都会被取消（指令/传送门不受影响）。 */
    @SubscribeEvent
    public static void onTeleport(net.minecraftforge.event.entity.EntityTeleportEvent event) {
        if (event.getEntity() instanceof LivingEntity living
                && living.hasEffect(ModEffects.ROOTED.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity().hasEffect(ModEffects.ENTANGLED.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity().hasEffect(ModEffects.ENTANGLED.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().hasEffect(ModEffects.ENTANGLED.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().hasEffect(ModEffects.ENTANGLED.get())) {
            event.setCanceled(true);
        }
    }
}
