package com.goetyarcana.combat;

import com.goetyarcana.registry.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 灵魂套的公共逻辑：四件套判定、额外心的存取、灵魂能量读写。
 */
public final class SoulArmor {

    /** 额外心数量（每颗 = 2 点血，最多 20 颗）。 */
    public static final String HEARTS_TAG = "goetyarcana.soul_hearts";
    public static final int MAX_HEARTS = 20;
    /** 满血时每 2 秒 +1 颗心，消耗 100 灵魂。 */
    public static final int HEART_COST = 100;
    public static final int HEART_INTERVAL = 40;
    /** 自愈：每点血消耗的灵魂。 */
    public static final int REGEN_SOUL_COST = 25;

    /** 额外心的最大生命修饰器。 */
    private static final java.util.UUID HEART_MODIFIER = java.util.UUID.fromString(
            "9b1f4a52-6a2e-4f18-9c3d-3f6c1a7e8d21");

    private SoulArmor() {
    }

    public static boolean isPiece(ItemStack stack) {
        Item item = stack.getItem();
        return item == ModItems.SOUL_HELMET.get() || item == ModItems.SOUL_CHESTPLATE.get()
                || item == ModItems.SOUL_LEGGINGS.get() || item == ModItems.SOUL_BOOTS.get();
    }

    public static boolean isFullSet(Player player) {
        return isPiece(player.getItemBySlot(EquipmentSlot.HEAD))
                && isPiece(player.getItemBySlot(EquipmentSlot.CHEST))
                && isPiece(player.getItemBySlot(EquipmentSlot.LEGS))
                && isPiece(player.getItemBySlot(EquipmentSlot.FEET));
    }

    public static int hearts(Player player) {
        return player.getPersistentData().getInt(HEARTS_TAG);
    }

    public static void setHearts(Player player, int value) {
        int clamped = net.minecraft.util.Mth.clamp(value, 0, MAX_HEARTS);
        player.getPersistentData().putInt(HEARTS_TAG, clamped);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.goetyarcana.network.ModNetwork.sendSoulHearts(serverPlayer.serverLevel(), player, clamped);
        }
        applyHeartModifier(player, clamped);
    }

    /** 兼容旧代码：现在不再改最大生命，改为伤害吸收式的灵魂护盾。 */
    public static void applyHeartModifier(Player player, int hearts) {
        // 什么都不做
    }

    /** 灵魂护盾点数（每颗心 2 点）。 */
    public static float shield(Player player) {
        return hearts(player) * 2.0F;
    }

    /** 用护盾吸收伤害，返回剩下的伤害。 */
    public static float absorb(Player player, float amount) {
        float shield = shield(player);
        if (shield <= 0.0F) {
            return amount;
        }
        float absorbed = Math.min(shield, amount);
        float left = shield - absorbed;
        setHearts(player, (int) Math.ceil(left / 2.0F));
        return amount - absorbed;
    }

    public static int soulEnergy(Player player) {
        return player.getCapability(
                        com.Polarice3.Goety.common.capabilities.soulenergy.SEProvider.CAPABILITY)
                .map(com.Polarice3.Goety.common.capabilities.soulenergy.ISoulEnergy::getSoulEnergy)
                .orElse(0);
    }

    public static boolean spendSoul(Player player, int amount) {
        var optional = player.getCapability(
                com.Polarice3.Goety.common.capabilities.soulenergy.SEProvider.CAPABILITY);
        if (optional.resolve().isEmpty()) {
            return false;
        }
        var energy = optional.resolve().get();
        if (energy.getSoulEnergy() < amount) {
            return false;
        }
        energy.setSoulEnergy(energy.getSoulEnergy() - amount);
        return true;
    }

    /**
     * 身上是否有可用的「复活类」物品（Goety 的灵魂方舟 / 哀嚎之魂 / 烈焰头盔等）。
     * 有的话让复活先触发，灵魂形态作为最后手段。
     */
    public static boolean hasReviveItem(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                continue;
            }
            String name = stack.getItem().getClass().getName();
            if (name.startsWith("com.Polarice3.Goety.common.items.revive")) {
                return true;
            }
            var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null && (id.getPath().contains("soul_jar")
                    || id.getPath().contains("howling_soul")
                    || id.getPath().contains("totem_of_souls"))) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            String name = stack.getItem().getClass().getName();
            if (!stack.isEmpty() && name.startsWith("com.Polarice3.Goety.common.items.revive")) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasSoulForm(Player player) {
        return player.hasEffect(com.goetyarcana.registry.ModEffects.SOUL_FORM.get());
    }

    public static void giveSoulForm(Player player) {
        player.addEffect(new MobEffectInstance(com.goetyarcana.registry.ModEffects.SOUL_FORM.get(),
                com.goetyarcana.effect.SoulFormEffect.DURATION, 0, false, true, true));
    }
}
