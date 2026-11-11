package com.goetyarcana.item;

import com.goetyarcana.VoidScytheBalance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 虚空镰刀的 Tier。
 *
 * <p>伤害与攻速都在 {@link VoidScytheItem} 的 {@code SwordItem} 构造参数里给，
 * 所以这里的攻击力加成为 0。具体面板：
 * 1（玩家基础）+ 12（武器）= 13 伤害；4.0（玩家基础）- 3.1 = 0.9 攻速。</p>
 */
public enum VoidScytheTier implements Tier {
    INSTANCE;

    @Override
    public int getUses() {
        return VoidScytheBalance.DURABILITY;
    }

    @Override
    public float getSpeed() {
        return 9.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 0.0F;
    }

    @Override
    public int getLevel() {
        return 4;
    }

    @Override
    public int getEnchantmentValue() {
        return VoidScytheBalance.ENCHANTABILITY;
    }

    @Override
    public Ingredient getRepairIngredient() {
        // 创造模式专属物品，但仍然允许用回响碎片修复，方便测试。
        return Ingredient.of(Items.ECHO_SHARD);
    }
}
