package com.goetyarcana.event;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.registry.ModEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 砰砰诅咒：受到的所有伤害 +40%，其中爆炸伤害 +80%。
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BombCurseEvents {

    private static final float NORMAL_BONUS = 0.40F;
    private static final float EXPLOSION_BONUS = 0.80F;

    private BombCurseEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (!player.hasEffect(ModEffects.BOMB_CURSE.get())) {
            return;
        }
        float bonus = event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                ? EXPLOSION_BONUS : NORMAL_BONUS;
        event.setAmount(event.getAmount() * (1.0F + bonus));
    }
}
