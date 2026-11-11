package com.goetyarcana.combat;

import com.Polarice3.Goety.utils.MobUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * 战斗目标筛选：只打敌对/正在攻击玩家的生物，跳过玩家、盟友、宠物、
 * 盔甲架、已死亡和无敌状态的实体。
 */
public final class CombatUtil {

    public static boolean isValidTarget(@Nullable LivingEntity attacker, LivingEntity target) {
        if (target == attacker || target.isRemoved() || target.isDeadOrDying()) {
            return false;
        }
        if (target instanceof Player || target.isSpectator() || target.isInvulnerable()) {
            return false;
        }
        if (target instanceof ArmorStand) {
            return false;
        }
        if (attacker != null) {
            if (MobUtil.areAllies(attacker, target)) {
                return false;
            }
            if (target instanceof OwnableEntity ownable
                    && ownable.getOwnerUUID() != null
                    && ownable.getOwnerUUID().equals(attacker.getUUID())) {
                return false;
            }
        }

        // Goety 的 Boss 可能实现 NeutralMob 而不是 Enemy，因此先看 Boss 分级。
        if (CombatTier.isBoss(target)) {
            return true;
        }
        if (target instanceof Enemy || target.getType().getCategory() == MobCategory.MONSTER) {
            return true;
        }
        // 中立生物只在已经对攻击者产生敌意时才会被 360° 打到，
        // 避免路过时误伤村民、动物或未激怒的狼。
        return attacker != null
                && target instanceof NeutralMob neutral
                && neutral.isAngryAt(attacker);
    }

    private CombatUtil() {
    }
}
