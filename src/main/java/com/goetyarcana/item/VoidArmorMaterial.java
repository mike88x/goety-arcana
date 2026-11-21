package com.goetyarcana.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 虚空套材质：护甲 10/20/15/10、韧性 8/14/10/8，每件额外提供 20% 击退抗性（由事件里的属性修饰器实现）。
 */
public class VoidArmorMaterial implements ArmorMaterial {

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 700;
            case CHESTPLATE -> 1100;
            case LEGGINGS -> 950;
            case BOOTS -> 800;
        };
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 10;
            case CHESTPLATE -> 20;
            case LEGGINGS -> 15;
            case BOOTS -> 10;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 20;
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
        return "void";
    }

    @Override
    public float getToughness() {
        return 0.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }

    /** 按部位的韧性（接口只有一个总值，用属性修饰器逐件补）。 */
    public float toughnessFor(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 8.0F;
            case CHESTPLATE -> 14.0F;
            case LEGGINGS -> 10.0F;
            case BOOTS -> 8.0F;
        };
    }
}
