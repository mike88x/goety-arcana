package com.goetyarcana.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.goetyarcana.VoidScytheBalance;
import com.goetyarcana.entity.ScytheWaveEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import java.util.List;
import java.util.UUID;

/**
 * 虚空镰刀。
 *
 * <p>面板：13 伤害、0.9 攻速；左键会由 {@code ScytheEvents} 接管，
 * 对玩家周围 360° 的所有敌人各结算一次伤害。</p>
 *
 * <p>右键：按住开始蓄力，松开时如果蓄力达到最低时间，就生成一个
 * {@link ScytheWaveEntity}。蓄力越久，剑气的张角（90°→270°）、
 * 半径（6→12 格）和百分比伤害（5%→15%）都越高。</p>
 */
public class VoidScytheItem extends SwordItem {

    /** 与 4.5 格 360° 横扫匹配：玩家默认实体交互距离 3 格，额外 +1 格。 */
    private static final UUID REACH_MODIFIER_UUID =
            UUID.fromString("a4a6c9f8-4f0e-4a0e-9f8b-0e4a4f9c8b21");

    private final Multimap<Attribute, AttributeModifier> scytheAttributes;

    public VoidScytheItem() {
        super(VoidScytheTier.INSTANCE,
                12,                 // 玩家基础 1 + 12 = 13 伤害
                -3.1F,              // 玩家基础 4.0 - 3.1 = 0.9 攻速
                new Properties()
                        .stacksTo(1)
                        .durability(VoidScytheBalance.DURABILITY)
                        .rarity(Rarity.EPIC)
                        .fireResistant());

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(super.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND));
        builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(
                REACH_MODIFIER_UUID,
                "Void scythe reach",
                1.0D,
                AttributeModifier.Operation.ADDITION));
        this.scytheAttributes = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND
                ? this.scytheAttributes
                : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        // 允许一直举着镰刀维持满蓄力；松开时才结算。
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // 蓄力姿势完全由客户端 PlayerModelMixin 接管；这里用 NONE 避免原版
        // SPEAR 姿势和自定义双手姿势叠加。
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (level.isClientSide) {
            return;
        }

        int used = this.getUseDuration(stack) - remainingUseTicks;
        if (used == VoidScytheBalance.FULL_CHARGE_TICKS) {
            // 蓄满的提示音。刀片模型在同 tick 由 client 的 charge 属性切到发光版本。
            level.playSound(null, entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS,
                    0.8F, 1.45F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }

        int used = this.getUseDuration(stack) - timeLeft;
        if (used < VoidScytheBalance.MIN_CHARGE_TICKS || level.isClientSide) {
            return;
        }

        float charge = Mth.clamp(
                (used - VoidScytheBalance.MIN_CHARGE_TICKS)
                        / (float) (VoidScytheBalance.FULL_CHARGE_TICKS - VoidScytheBalance.MIN_CHARGE_TICKS),
                0.0F, 1.0F);

        // 剑气沿视线向前飞，不再是以玩家为中心的扇形。
        Vec3 direction = player.getViewVector(1.0F);
        ScytheWaveEntity wave = new ScytheWaveEntity(level, player, charge, direction);
        level.addFreshEntity(wave);

        float pitch = 0.65F + charge * 0.35F;
        level.playSound(null, player.getX(), player.getY() + 1.0D, player.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.9F, pitch);
        level.playSound(null, player.getX(), player.getY() + 1.0D, player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, pitch);

        player.getCooldowns().addCooldown(this, VoidScytheBalance.WAVE_COOLDOWN_TICKS);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.goetyarcana.void_scythe.stats")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.goetyarcana.void_scythe.charge")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.goetyarcana.void_scythe.effects")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.goetyarcana.void_scythe.creative_only")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
