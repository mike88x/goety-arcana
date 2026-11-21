package com.goetyarcana.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 灵魂套的护甲材质：护甲 4/8/7/4、韧性 1/3/2/1、附魔能力 10、耐久 250/430/330/220。
 */
public class SoulArmorMaterial implements ArmorMaterial {

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 250;
            case CHESTPLATE -> 430;
            case LEGGINGS -> 330;
            case BOOTS -> 220;
        };
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 4;
            case CHESTPLATE -> 8;
            case LEGGINGS -> 7;
            case BOOTS -> 4;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 10;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }

    @Override
    public String getName() {
        return "soul";
    }

    @Override
    public float getToughness() {
        // 逐件韧性由 ArcanaArmorItem 的属性修饰器给出（1/3/2/1），这里给 0 避免重复显示
        return 0.0F;
    }

    /** 按部位区分的韧性（接口只给一个总值，这里在装备时另加修正，见 SoulArmorEvents）。 */
    public float toughnessFor(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 1.0F;
            case CHESTPLATE -> 3.0F;
            case LEGGINGS -> 2.0F;
            case BOOTS -> 1.0F;
        };
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }

    public ItemStack repairStack() {
        return ItemStack.EMPTY;
    }
}
