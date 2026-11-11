package com.goetyarcana.spell;

import com.Polarice3.Goety.api.magic.SpellType;
import com.Polarice3.Goety.common.enchantments.ModEnchantments;
import com.Polarice3.Goety.common.magic.Spell;
import com.Polarice3.Goety.common.magic.SpellStat;
import com.goetyarcana.SoulBurstBalance;
import com.goetyarcana.entity.SoulBurstEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 裂魂聚晶对应的法术。
 *
 * 继承 Goety 的 {@link Spell}（已实现 {@code ISpell} 的全部默认行为：
 * 灵魂能量消耗、吟唱、冷却、法杖类型判定、粒子等），
 * 只需要提供数值并在 {@link #SpellResult} 里生成射弹。
 */
public class SoulBurstSpell extends Spell {

    @Override
    public int defaultSoulCost() {
        return SoulBurstBalance.SOUL_COST;
    }

    @Override
    public int defaultCastDuration() {
        return SoulBurstBalance.CAST_DURATION;
    }

    @Override
    public int defaultSpellCooldown() {
        return SoulBurstBalance.COOLDOWN;
    }

    /**
     * 虚空（VOID）系。
     *
     * Goety 的 {@code IWand.cannotCast()} 只校验冷却与 {@code conditionsMet}，
     * 并不校验法杖类型，所以任何法杖都能施放这枚聚晶；
     * 学派只影响施法粒子、灵魂能量折扣（在末地/虚空生物群系更便宜）
     * 以及 {@link #typeStaff} 分支——也就是下面「虚空魔杖额外效果」的判定。
     */
    @Override
    public SpellType getSpellType() {
        return SpellType.VOID;
    }

    /** 允许附上「强效」附魔，每级 +1 固定伤害（见 SoulBurstBalance.POTENCY_BONUS 用法）。 */
    @Override
    public List<Enchantment> acceptedEnchantments() {
        List<Enchantment> list = new ArrayList<>();
        list.add(ModEnchantments.POTENCY.get());
        return list;
    }

    @Override
    public void SpellResult(ServerLevel level, LivingEntity caster, ItemStack staff, SpellStat spellStat) {
        Vec3 look = caster.getViewVector(1.0F);
        // 从「准星」出发：眼睛位置 + 视线方向偏移，射弹会正好从屏幕中心飞出去，
        // 而不是从身体中心/肩膀旁边冒出来。
        Vec3 spawn = caster.getEyePosition(1.0F).add(look.scale(SoulBurstBalance.SPAWN_OFFSET));

        SoulBurstEntity bolt = new SoulBurstEntity(level, caster, spawn, look);
        bolt.setVoidCharged(this.typeStaff(staff, SpellType.VOID));

        int potency = spellStat.getPotency();
        bolt.setExtraDamage(potency * SoulBurstBalance.POTENCY_BONUS);
        bolt.setOwner(caster);

        level.addFreshEntity(bolt);
        this.playSound(level, caster, 1.0F, 1.2F);
    }
}
