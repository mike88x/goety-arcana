package com.goetyarcana.item;

import com.goetyarcana.GoetyArcanaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 带自定义贴图的盔甲：材质来自原版（先做样子），贴图路径改成
 * {@code goetyarcana:textures/models/armor/<set>_layer_1|2.png}。
 */
public class ArcanaArmorItem extends ArmorItem {

    private final String set;

    public ArcanaArmorItem(ArmorMaterial material, Type type, Properties properties, String set) {
        super(material, type, properties);
        this.set = set;
    }

    /** 套装名：void / soul。 */
    public String armorSet() {
        return this.set;
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,
            net.minecraft.world.entity.ai.attributes.AttributeModifier> getDefaultAttributeModifiers(
            net.minecraft.world.entity.EquipmentSlot slot) {
        var base = super.getDefaultAttributeModifiers(slot);
        if (slot != this.getType().getSlot()) {
            return base;
        }
        if ("soul".equals(this.armorSet())) {
            float extra = switch (this.getType()) {
                case HELMET -> 1.0F;
                case CHESTPLATE -> 3.0F;
                case LEGGINGS -> 2.0F;
                case BOOTS -> 1.0F;
            };
            if (extra <= 0.0F) {
                return base;
            }
            var soulModifiers = com.google.common.collect.LinkedHashMultimap.create(base);
            soulModifiers.put(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            java.util.UUID.fromString("c5e1a7b2-3d94-4f68-8a17-6b2c9d4e1f37"),
                            "goetyarcana:soul_toughness_item" + slot.getName(), extra,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
            return soulModifiers;
        }
        if (!"void".equals(this.armorSet())) {
            return base;
        }
        // 虚空套逐件：韧性 8/14/10/8，击退抗性 20%
        float toughness = switch (this.getType()) {
            case HELMET -> 8.0F;
            case CHESTPLATE -> 14.0F;
            case LEGGINGS -> 10.0F;
            case BOOTS -> 8.0F;
        };
        var modifiers = com.google.common.collect.LinkedHashMultimap.create(base);
        modifiers.put(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        java.util.UUID.fromString("6f1d3a84-2c5e-4b17-9d3a-8e2b4c6f1a55"),
                        "goetyarcana:void_toughness_item" + slot.getName(), toughness,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        modifiers.put(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        java.util.UUID.fromString("a2c7e519-4f8b-4d63-8e1a-5b9c3d7f2e48"),
                        "goetyarcana:void_knockback_item" + slot.getName(), 0.2D,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level,
                                java.util.List<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null) {
            tooltip.add(net.minecraft.network.chat.Component
                    .translatable("item." + id.getNamespace() + "." + id.getPath() + ".info")
                    .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
        }
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // 穿 Goety 长袍时，长袍会盖住腿；这时护腿改用透明层，避免两层布面穿模闪面
        String layer = slot == EquipmentSlot.LEGS ? "layer_2" : "layer_1";
        return new ResourceLocation(GoetyArcanaMod.MOD_ID,
                "textures/models/armor/" + this.set + "_" + layer + ".png").toString();
    }
}
