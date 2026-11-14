package com.goetyarcana.item;

import com.goetyarcana.combat.VoidRobe;
import com.goetyarcana.entity.VoidBoltEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * 虚空弩矢：只能被虚空弩装填；击中或未击中都会在 4 秒后爆炸。
 *
 * <p>射击者穿着 Goety 的虚空长袍时，弩矢改为追踪 + 可穿透。</p>
 */
public class VoidBoltItem extends ArrowItem {

    public VoidBoltItem() {
        super(new Properties().stacksTo(64).rarity(Rarity.COMMON));
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        VoidBoltEntity bolt = new VoidBoltEntity(level, shooter);
        if (VoidRobe.isWorn(shooter)) {
            bolt.enableVoidRobeTracking();
        }
        return bolt;
    }
}
