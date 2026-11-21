package com.goetyarcana.event;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.combat.SoulArmor;
import com.goetyarcana.registry.ModDamageTypes;
import com.goetyarcana.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 灵魂套的三条套装效果：
 *
 * <ol>
 *   <li>命中 66% 概率额外造成 200% 灵魂伤害；</li>
 *   <li>用灵魂能量自愈；满血时每 2 秒花 100 灵魂换一颗额外心（最多 20 颗），
 *       攻击时最多消耗 5 颗，每颗 +10% 伤害；</li>
 *   <li>受到致命伤害时进入灵魂形态 10 秒（伤害转灵魂、攻击 +200%），结束后死亡。
 *       优先级低于虚空护身符（护符先处理，见 {@link VoidAmuletEvents}）。</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SoulArmorEvents {

    private static final float SOUL_PROC_CHANCE = 0.66F;
    private static final float SOUL_PROC_MULTIPLIER = 2.0F;
    private static final int MAX_SPEND_HEARTS = 5;

    /** 灵魂套逐件韧性修正（材质接口只能给统一 1 点，这里补到 1/3/2/1）。 */
    private static final java.util.UUID SOUL_TOUGHNESS_UUID = java.util.UUID.fromString(
            "3e7d1c48-9b2a-4f16-8d5c-1a4b6f2e7c93");

    private SoulArmorEvents() {
    }

    // ---------------------------------------------------------------- 每秒结算：自愈 / 攒心

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player) || !SoulArmor.isFullSet(player)) {
            return;
        }
        long time = player.level().getGameTime();
        if (time % 20 == 0) {
            // 自愈：没满血就花灵魂回血；最近受过伤则更快
            if (player.getHealth() < player.getMaxHealth()) {
                boolean recentlyHurt = player.getLastHurtByMobTimestamp() != 0
                        && player.tickCount - player.getLastHurtByMobTimestamp() < 100;
                if (recentlyHurt) {
                    if (SoulArmor.spendSoul(player, SoulArmor.REGEN_SOUL_COST / 2)) {
                        player.heal(1.0F);
                    }
                } else if (time % 40 == 0 && SoulArmor.spendSoul(player, SoulArmor.REGEN_SOUL_COST)) {
                    player.heal(1.0F);
                }
            }
        }

        // 满血时攒额外心
        if (time % SoulArmor.HEART_INTERVAL == 0
                && player.getHealth() >= player.getMaxHealth()
                && SoulArmor.hearts(player) < SoulArmor.MAX_HEARTS) {
            if (SoulArmor.spendSoul(player, SoulArmor.HEART_COST)) {
                SoulArmor.setHearts(player, SoulArmor.hearts(player) + 1);
            }
        }
    }

    // ---------------------------------------------------------------- 攻击

    /** 灵魂护盾先吃伤害（类似伤害吸收）。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onShield(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (!SoulArmor.isFullSet(player)) {
            return;
        }
        float left = SoulArmor.absorb(player, event.getAmount());
        event.setAmount(left);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || player.level().isClientSide() || event.getEntity() == player) {
            return;
        }
        if (!SoulArmor.isFullSet(player)) {
            return;
        }

        float amount = event.getAmount();

        // 灵魂形态：攻击 +200%
        if (SoulArmor.hasSoulForm(player)) {
            amount *= 3.0F;
        }

        // 灵魂伤害/追加伤害不再触发消耗
        if (event.getSource().is(ModDamageTypes.SOUL)) {
            return;
        }
        // 消耗额外心：一次挥击只扣一次（4 tick 内不再扣，避免横扫/追加连续触发）
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong("goetyarcana.soul_spend_tick");
        if (now - last >= 4) {
            int hearts = SoulArmor.hearts(player);
            int spend = Math.min(MAX_SPEND_HEARTS, hearts);
            if (spend > 0) {
                player.getPersistentData().putLong("goetyarcana.soul_spend_tick", now);
                SoulArmor.setHearts(player, hearts - spend);
                amount *= 1.0F + 0.10F * spend;
            }
        }

        event.setAmount(amount);
    }

    /** 66% 概率的 200% 灵魂伤害（在伤害结算之后追加）。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSoulProc(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || player.level().isClientSide() || event.getEntity() == player) {
            return;
        }
        if (!SoulArmor.isFullSet(player)) {
            return;
        }
        // 灵魂形态下整段伤害已经视作灵魂伤害，不重复触发
        if (SoulArmor.hasSoulForm(player)) {
            convertToSoul(event, player);
            return;
        }
        if (player.getRandom().nextFloat() >= SOUL_PROC_CHANCE) {
            return;
        }
        LivingEntity target = event.getEntity();
        float extra = event.getAmount() * SOUL_PROC_MULTIPLIER;
        if (target.isAlive()) {
            DamageSource source = ModDamageTypes.source(target.level(),
                    ModDamageTypes.SOUL, player, player);
            target.invulnerableTime = 0;
            target.hurt(source, extra);
            if (target.level() instanceof net.minecraft.server.level.ServerLevel server) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        14, 0.35D, 0.5D, 0.35D, 0.03D);
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                        target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                        8, 0.3D, 0.4D, 0.3D, 0.01D);
                server.playSound(null, target.blockPosition(),
                        net.minecraft.sounds.SoundEvents.SOUL_ESCAPE,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.4F);
            }
        }
    }

    /** 灵魂形态：把整段伤害改写成灵魂伤害（先清零再打一次）。 */
    private static void convertToSoul(LivingDamageEvent event, Player player) {
        if (event.getSource().is(ModDamageTypes.SOUL)) {
            return;
        }
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        event.setAmount(0.0F);
        if (target.isAlive()) {
            DamageSource source = ModDamageTypes.source(target.level(),
                    ModDamageTypes.SOUL, player, player);
            target.invulnerableTime = 0;
            target.hurt(source, amount);
        }
    }

    // ---------------------------------------------------------------- 致命伤害 → 灵魂形态

    /** 优先级 LOWEST：虚空护身符（LOW）先处理；护符救下后这里就不再是致命伤害。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFatalDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (event.getAmount() < player.getHealth()) {
            return;
        }
        if (!SoulArmor.isFullSet(player) || SoulArmor.hasSoulForm(player)) {
            return;
        }
        // 优先让复活类物品生效（灵魂方舟等），灵魂形态是最后手段
        if (SoulArmor.hasReviveItem(player)) {
            return;
        }
        event.setAmount(Math.max(0.0F, player.getHealth() - 1.0F));
        SoulArmor.giveSoulForm(player);
        player.displayClientMessage(net.minecraft.network.chat.Component
                .translatable("message.goetyarcana.soul_form.start"), true);
    }
}
