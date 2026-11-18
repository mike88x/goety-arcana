package com.goetyarcana.entity;

import com.goetyarcana.VoidBlastBalance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * 附魔虚空苦力怕仆从。
 *
 * <p>和普通虚空苦力怕仆从完全同源（血量、护甲、韧性、索敌、行为一致），区别只有：</p>
 * <ul>
 *   <li>爆炸伤害 = 原版闪电苦力怕困难模式最大伤害的 75%；</li>
 *   <li>没炸死的目标拿到的砰砰诅咒是**二级**：普通怪照样压到 1 血，但诅咒结束时先挨一点伤害；
 *       精英 / BOSS 的爆炸加成从 +50% 提到 +100%；</li>
 *   <li>外观是附魔版（符文发光）。</li>
 * </ul>
 */
public class EnchantedVoidCreeperServant extends VoidCreeperServant {

    public EnchantedVoidCreeperServant(EntityType<? extends EnchantedVoidCreeperServant> type, Level level) {
        super(type, level);
    }

    public EnchantedVoidCreeperServant(Level level, LivingEntity owner) {
        this(com.goetyarcana.registry.ModEntities.ENCHANTED_VOID_CREEPER_SERVANT.get(), level);
        this.setOwnerId(owner.getUUID());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,
                        VoidBlastBalance.SERVANT_HEALTH)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                        VoidBlastBalance.SERVANT_ARMOR)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS,
                        VoidBlastBalance.SERVANT_TOUGHNESS)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE,
                        VoidBlastBalance.SERVANT_RANGE)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    protected float explosionDamage() {
        return VoidBlastBalance.ENCHANTED_EXPLOSION_DAMAGE;
    }

    @Override
    protected float curseBonus() {
        return VoidBlastBalance.CURSE_BLAST_BONUS_ENCHANTED;
    }

    @Override
    protected int curseAmplifier() {
        return 1;
    }

    @Override
    public net.minecraft.network.chat.Component getName() {
        return super.getName();
    }
}
