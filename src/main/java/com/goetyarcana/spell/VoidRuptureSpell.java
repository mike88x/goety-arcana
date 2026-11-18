package com.goetyarcana.spell;

import com.Polarice3.Goety.api.magic.SpellType;
import com.Polarice3.Goety.common.magic.Spell;
import com.Polarice3.Goety.common.magic.SpellStat;
import com.goetyarcana.VoidBlastBalance;
import com.goetyarcana.combat.VoidRobe;
import com.goetyarcana.entity.VoidCreeperServant;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 虚空崩爆：在施法者前方召唤一只虚空苦力怕仆从。
 *
 * <p>属于虚空（VOID）系。</p>
 */
public class VoidRuptureSpell extends Spell {

    @Override
    public int defaultSoulCost() {
        return VoidBlastBalance.SOUL_COST;
    }

    @Override
    public int defaultCastDuration() {
        return VoidBlastBalance.CAST_DURATION;
    }

    @Override
    public int defaultSpellCooldown() {
        return VoidBlastBalance.SPELL_COOLDOWN;
    }

    @Override
    public SpellType getSpellType() {
        return SpellType.VOID;
    }

    @Override
    public void SpellResult(ServerLevel level, LivingEntity caster, ItemStack staff, SpellStat spellStat) {
        boolean voidStaff = this.typeStaff(staff, SpellType.VOID);
        int count = VoidRobe.isWorn(caster)
                ? VoidBlastBalance.SUMMON_COUNT_ROBE
                : VoidBlastBalance.SUMMON_COUNT;
        Vec3 look = caster.getViewVector(1.0F);
        Vec3 base = caster.position().add(look.scale(2.0D));
        for (int index = 0; index < count; index++) {
            double angle = caster.getYRot() * Math.PI / 180.0 + index * (Math.PI * 2.0 / count);
            double radius = count >= 6 ? 2.0D : 1.1D;
            Vec3 spawn = base.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            boolean enchanted = voidStaff
                    && caster.getRandom().nextFloat() < VoidBlastBalance.ENCHANTED_CHANCE;
            VoidCreeperServant servant = enchanted
                    ? new com.goetyarcana.entity.EnchantedVoidCreeperServant(level, caster)
                    : new VoidCreeperServant(level, caster);
            servant.setPos(spawn.x, spawn.y, spawn.z);
            // 对着地面施法时会把仆从生成在方块里（半截入土），这里抬出来
            for (int i = 0; i < 12 && !level.noCollision(servant, servant.getBoundingBox()); i++) {
                servant.setPos(spawn.x, spawn.y + 0.2D * (i + 1), spawn.z);
            }
            level.addFreshEntity(servant);

            level.sendParticles(ParticleTypes.PORTAL,
                    spawn.x, spawn.y + 0.6D, spawn.z, 30, 0.5D, 0.8D, 0.5D, 0.15D);
            level.sendParticles(com.goetyarcana.registry.ModParticles.VOID_FLAME.get(),
                    spawn.x, spawn.y + 0.4D, spawn.z, 18, 0.4D, 0.5D, 0.4D, 0.03D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    spawn.x, spawn.y + 0.6D, spawn.z, 20, 0.3D, 0.5D, 0.3D, 0.1D);
        }
        level.playSound(null, base.x, base.y, base.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, base.x, base.y, base.z,
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 0.8F, 0.7F);
        this.playSound(level, caster, 1.0F, 0.9F);
    }
}
