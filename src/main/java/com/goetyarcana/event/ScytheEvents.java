package com.goetyarcana.event;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.combat.ScytheCombat;
import com.goetyarcana.item.VoidScytheItem;
import com.goetyarcana.network.ModNetwork;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 左键入口。
 *
 * <ul>
 *   <li>{@link AttackEntityEvent}：点到实体时取消原版单体攻击，换成 360° 横扫；</li>
 *   <li>{@link PlayerInteractEvent.LeftClickBlock}：点到方块时同样横扫，并阻止镰刀挖方块；</li>
 *   <li>{@link PlayerInteractEvent.LeftClickEmpty}：客户端空挥，发包给服务端横扫。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScytheEvents {

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (player.getMainHandItem().getItem() instanceof VoidScytheItem) {
            event.setCanceled(true);
            ScytheCombat.trySweep(player);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!(player.getMainHandItem().getItem() instanceof VoidScytheItem)) {
            return;
        }

        // 镰刀是武器，不拿来挖方块；同时让这次左键变成横扫。
        event.setCanceled(true);
        if (!player.level().isClientSide) {
            ScytheCombat.trySweep(player);
        }
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!player.level().isClientSide
                || !(stack.getItem() instanceof VoidScytheItem)) {
            return;
        }
        // 冷却未好时不必发包，减少无意义流量。
        if (player.getAttackStrengthScale(0.5F) >= 0.9F) {
            ModNetwork.sendScytheSwing();
        }
    }

    private ScytheEvents() {
    }
}
