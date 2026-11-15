package com.goetyarcana.item;

import com.goetyarcana.registry.ModEffects;
import com.goetyarcana.registry.ModItems;
import com.goetyarcana.registry.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/**
 * 虚空弩：装填阈值随虚空之眼层数提前，箭速随层数提高。
 *
 * <p>弹药：只要身上（手持或背包）还有虚空弩矢就优先用弩矢，没有弩矢时退化成普通箭
 * （箭/药箭/光灵箭等一切 {@link ArrowItem}）。</p>
 */
public class VoidCrossbowItem extends CrossbowItem {

    private static final float BASE_ARROW_SPEED = 1.6F;

    /**
     * 装填期间“为谁装填”。
     *
     * <p>原版 {@code LivingEntity#getProjectile} 只用两个 {@link Predicate} 挑弹药，
     * 谓词只拿到 {@link ItemStack}、拿不到射击者，没法表达“有弩矢就优先弩矢”。
     * 而 {@code use}/{@code releaseUsing} 是原版仅有的两处挑弹药的地方，
     * 所以在这两段里把射击者放进 ThreadLocal，让谓词能查到优先级。</p>
     */
    private static final ThreadLocal<LivingEntity> LOADING_FOR = new ThreadLocal<>();

    /** 弩矢永远可用；普通箭只在没有任何弩矢时才可用。 */
    private static final Predicate<ItemStack> PROJECTILES = stack -> {
        if (isBolt(stack)) {
            return true;
        }
        if (!(stack.getItem() instanceof ArrowItem)) {
            return false;
        }
        LivingEntity shooter = LOADING_FOR.get();
        return shooter == null || !hasBolt(shooter);
    };

    public VoidCrossbowItem() {
        super(new Properties()
                .stacksTo(1)
                .durability(465)
                .rarity(Rarity.RARE));
    }

    private static boolean isBolt(ItemStack stack) {
        return stack.is(ModItems.VOID_CROSSBOW_BOLT.get());
    }

    /** 手持（主/副）或背包里是否还有虚空弩矢。 */
    private static boolean hasBolt(LivingEntity shooter) {
        if (isBolt(shooter.getMainHandItem()) || isBolt(shooter.getOffhandItem())) {
            return true;
        }
        if (shooter instanceof Player player) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (isBolt(player.getInventory().getItem(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return PROJECTILES;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return PROJECTILES;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean wasCharged = CrossbowItem.isCharged(stack);

        // 原版 use() 里会调用 getProjectile 选弹药，借这段时间把射击者交给谓词。
        InteractionResultHolder<ItemStack> result;
        LOADING_FOR.set(player);
        try {
            result = super.use(level, player, hand);
        } finally {
            LOADING_FOR.remove();
        }

        if (wasCharged && !CrossbowItem.isCharged(result.getObject())) {
            muzzle(player, level);
        }
        return result;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            LOADING_FOR.set(entity);
            try {
                super.releaseUsing(stack, level, entity, timeLeft);
            } finally {
                LOADING_FOR.remove();
            }
            return;
        }

        boolean wasCharged = CrossbowItem.isCharged(stack);
        MobEffectInstance eye = player.getEffect(ModEffects.VOID_EYE.get());
        int stacks = eye == null ? 0 : Math.min(3, eye.getAmplifier() + 1);

        boolean fastShot = false;
        if (stacks > 0) {
            int required = Math.max(1, Math.round(CrossbowItem.getChargeDuration(stack)
                    * (1.0F - 0.2F * stacks)));
            int used = getUseDuration(stack) - timeLeft;
            fastShot = used >= required && wasCharged;
        }

        LOADING_FOR.set(player);
        try {
            if (fastShot) {
                CrossbowItem.performShooting(level, entity, entity.getUsedItemHand(),
                        stack, BASE_ARROW_SPEED * (1.0F + 0.2F * stacks), 1.0F);
            } else {
                super.releaseUsing(stack, level, entity, timeLeft);
            }
        } finally {
            LOADING_FOR.remove();
        }

        if (wasCharged && !CrossbowItem.isCharged(stack)) {
            muzzle(player, level);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.goetyarcana.void_crossbow.lore")
                .withStyle(ChatFormatting.GRAY));
    }

    private static void muzzle(Player player, Level level) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        server.sendParticles(ModParticles.VOID_FLAME.get(),
                player.getX() + look.x * 2.0D,
                player.getEyeY() - 0.3D + look.y * 1.5D,
                player.getZ() + look.z * 2.0D,
                30, 0.3D, 0.3D, 0.3D, 0.05D);
        server.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS, 0.7F, 1.4F);
    }
}
