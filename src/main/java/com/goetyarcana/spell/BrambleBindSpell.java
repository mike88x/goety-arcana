package com.goetyarcana.spell;

import com.Polarice3.Goety.api.magic.SpellType;
import com.Polarice3.Goety.common.magic.Spell;
import com.Polarice3.Goety.common.magic.SpellStat;
import com.goetyarcana.BrambleBindBalance;
import com.goetyarcana.combat.WildRobe;
import com.goetyarcana.entity.BrambleSeedEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 荆棘藤缚对应的法术：射出一颗种子，落地后变成等待陷阱。
 *
 * <p>属于荒野（WILD）系，和虚空系无关。</p>
 *
 * <p>加成：拿荒野魔杖施法 → 范围 7×7、缠绕 15 秒；穿荒野长袍 → 范围 9×9，
 * 并且缠绕附带藤毒（每秒递增、无视免疫与护甲）。长袍的范围覆盖魔杖。</p>
 */
public class BrambleBindSpell extends Spell {

    @Override
    public int defaultSoulCost() {
        return BrambleBindBalance.SOUL_COST;
    }

    @Override
    public int defaultCastDuration() {
        return BrambleBindBalance.CAST_DURATION;
    }

    @Override
    public int defaultSpellCooldown() {
        return BrambleBindBalance.SPELL_COOLDOWN;
    }

    @Override
    public SpellType getSpellType() {
        return SpellType.WILD;
    }

    @Override
    public void SpellResult(ServerLevel level, LivingEntity caster, ItemStack staff, SpellStat spellStat) {
        boolean wildStaff = this.typeStaff(staff, SpellType.WILD);
        boolean robe = WildRobe.isWorn(caster);

        double radius = robe ? BrambleBindBalance.ROBE_RADIUS
                : wildStaff ? BrambleBindBalance.STAFF_RADIUS
                : BrambleBindBalance.CHECK_RADIUS;
        int ticks = wildStaff ? BrambleBindBalance.STAFF_ENTANGLE_TICKS
                : BrambleBindBalance.ENTANGLE_TICKS;
        int maxTargets = wildStaff ? BrambleBindBalance.STAFF_MAX_TARGETS
                : BrambleBindBalance.MAX_TARGETS;

        Vec3 look = caster.getViewVector(1.0F);
        BrambleSeedEntity seed = new BrambleSeedEntity(level, caster);
        seed.setPos(caster.getEyePosition(1.0F).add(look.scale(0.5D)).add(0.0D, -0.15D, 0.0D));
        seed.configure(radius, ticks, robe, maxTargets);
        seed.shoot(look.x, look.y, look.z, BrambleBindBalance.SEED_VELOCITY, 0.6F);
        level.addFreshEntity(seed);
        this.playSound(level, caster, 1.0F, 1.3F);
    }
}
