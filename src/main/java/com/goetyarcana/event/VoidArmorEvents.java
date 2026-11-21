package com.goetyarcana.event;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.registry.ModDamageTypes;
import com.goetyarcana.registry.ModEffects;
import com.goetyarcana.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 虚空套的五条技能。
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VoidArmorEvents {

    private static final UUID TOUGHNESS_UUID = UUID.fromString("2b0f6c3a-4d1e-4a6b-9d2f-7c3e5a8b1d42");
    private static final UUID KNOCKBACK_UUID = UUID.fromString("8c4d2a91-3f6b-4e21-a7d5-6b2f9e0c3a55");
    private static final UUID MARK_ARMOR_UUID = UUID.fromString("5f1c8d20-9a3b-4e77-8f1c-2d6b4a9e7c31");
    private static final UUID MARK_DAMAGE_UUID = UUID.fromString("d4b7e2f1-6c8a-4a13-9e5d-1f3b8c7a2e69");

    /** 每 20 灵魂抵 1 点伤害。 */
    private static final float SOUL_PER_DAMAGE = 20.0F;
    /** 动态减伤：每层 +6%，上限 60%。 */
    private static final float DR_STEP = 0.06F;
    private static final float DR_CAP = 0.60F;
    /** 印记起爆后的内置冷却（tick），期间不再叠层。 */
    private static final int MARK_CD_TICKS = 100;
    private static final String MARK_CD_TAG = "goetyarcana.void.mark_cd";
    private static final String MARK_APPLIED_TAG = "goetyarcana.void.mark_applied";

    private VoidArmorEvents() {
    }

    private static boolean isVoidPiece(ItemStack stack) {
        return stack.getItem() == ModItems.VOID_HELMET.get()
                || stack.getItem() == ModItems.VOID_CHESTPLATE.get()
                || stack.getItem() == ModItems.VOID_LEGGINGS.get()
                || stack.getItem() == ModItems.VOID_BOOTS.get();
    }

    public static boolean isFullSet(Player player) {
        return isVoidPiece(player.getItemBySlot(EquipmentSlot.HEAD))
                && isVoidPiece(player.getItemBySlot(EquipmentSlot.CHEST))
                && isVoidPiece(player.getItemBySlot(EquipmentSlot.LEGS))
                && isVoidPiece(player.getItemBySlot(EquipmentSlot.FEET));
    }

    private static int soul(Player player) {
        return player.getCapability(com.Polarice3.Goety.common.capabilities.soulenergy.SEProvider.CAPABILITY)
                .map(com.Polarice3.Goety.common.capabilities.soulenergy.ISoulEnergy::getSoulEnergy)
                .orElse(0);
    }

    private static boolean spendSoul(Player player, int amount) {
        var optional = player.getCapability(
                com.Polarice3.Goety.common.capabilities.soulenergy.SEProvider.CAPABILITY);
        if (optional.resolve().isEmpty()) {
            return false;
        }
        var energy = optional.resolve().get();
        if (energy.getSoulEnergy() < amount) {
            return false;
        }
        energy.setSoulEnergy(energy.getSoulEnergy() - amount);
        return true;
    }

    // ---------------------------------------------------------------- 每次 tick：修饰器 / 修甲 / 减伤衰减

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        Player player = event.player;
        long time = player.level().getGameTime();
        if (time % 20 != 0) {
            return;
        }
        // 技能 1：有灵魂能量就自动修甲（每 20 耐久花 1 灵魂）
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!isVoidPiece(stack) || !stack.isDamaged()) {
                continue;
            }
            int repaired = Math.min(stack.getDamageValue(), 20);
            if (spendSoul(player, 1)) {
                stack.setDamageValue(stack.getDamageValue() - repaired);
            }
        }

        // 技能 4：动态减伤随时间衰减
        CompoundTag data = player.getPersistentData();
        CompoundTag dr = data.getCompound("goetyarcana.void.dr");
        if (!dr.isEmpty()) {
            boolean changed = false;
            for (String key : new java.util.ArrayList<>(dr.getAllKeys())) {
                int value = dr.getInt(key);
                if (value > 0) {
                    dr.putInt(key, Math.max(0, value - 1));
                    changed = true;
                }
            }
            if (changed) {
                data.put("goetyarcana.void.dr", dr);
            }
        }
    }

    /** 虚空印记自然结束时，把减防/减伤修饰器还回去。 */
    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity.tickCount % 20 != 0) {
            return;
        }
        if (!entity.getPersistentData().getBoolean(MARK_APPLIED_TAG)) {
            return;
        }
        if (!entity.hasEffect(ModEffects.VOID_MARK.get())) {
            clearMarkModifiers(entity);
        }
    }

    // ---------------------------------------------------------------- 受伤：灵魂护盾（技能 3）+ 动态减伤（技能 4）

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (!isFullSet(player)) {
            return;
        }
        float amount = event.getAmount();

        // 动态减伤
        String typeKey = event.getSource().typeHolder().unwrapKey()
                .map(key -> key.location().toString()).orElse("generic");
        CompoundTag data = player.getPersistentData();
        CompoundTag dr = data.getCompound("goetyarcana.void.dr");
        float reduction = Math.min(DR_CAP, dr.getInt(typeKey) * DR_STEP);
        amount *= (1.0F - reduction);
        dr.putInt(typeKey, Math.min((int) (DR_CAP / DR_STEP), dr.getInt(typeKey) + 1));
        data.put("goetyarcana.void.dr", dr);

        // 灵魂能量抵伤：20 灵魂 = 1 点
        int available = soul(player);
        if (available > 0 && amount > 0.0F) {
            float absorb = Math.min(amount, available / SOUL_PER_DAMAGE);
            int cost = (int) Math.ceil(absorb * SOUL_PER_DAMAGE);
            if (spendSoul(player, cost)) {
                amount -= absorb;
            }
        }
        event.setAmount(Math.max(0.0F, amount));
    }

    // ---------------------------------------------------------------- 命中：虚空印记（技能 5）

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDealDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || player.level().isClientSide() || event.getEntity() == player) {
            return;
        }
        if (!isFullSet(player)) {
            return;
        }
        // 印记起爆伤害 / 灵魂伤害不再反过来叠印记
        if (event.getSource().is(ModDamageTypes.VOID_BLAST)
                || event.getSource().is(ModDamageTypes.SOUL)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (!target.isAlive()) {
            return;
        }
        long now = player.level().getGameTime();
        if (target.getPersistentData().getLong(MARK_CD_TAG) > now) {
            return;
        }
        MobEffectInstance current = target.getEffect(ModEffects.VOID_MARK.get());
        int stacks = current == null ? 1 : Math.min(5, current.getAmplifier() + 2);
        target.addEffect(new MobEffectInstance(ModEffects.VOID_MARK.get(), 200, stacks - 1,
                false, true, true));
        applyMarkModifiers(target, stacks);
        if (stacks >= 5) {
            target.removeEffect(ModEffects.VOID_MARK.get());
            clearMarkModifiers(target);
            float damage = target.getMaxHealth() * 0.25F;
            // 先写冷却再打伤害：这次伤害会再次进入本方法，必须已经处于冷却中
            target.getPersistentData().putLong(MARK_CD_TAG, now + MARK_CD_TICKS);
            var source = ModDamageTypes.source(target.level(), ModDamageTypes.VOID_BLAST, player, player);
            target.invulnerableTime = 0;
            target.hurt(source, damage);
            if (target.level() instanceof ServerLevel server) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                        target.getX(), target.getY() + 1.0D, target.getZ(), 30, 0.5D, 0.8D, 0.5D, 0.12D);
            }
        }
    }

    private static void applyMarkModifiers(LivingEntity target, int stacks) {
        target.getPersistentData().putBoolean(MARK_APPLIED_TAG, true);
        var armor = target.getAttribute(Attributes.ARMOR);
        var damage = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (armor != null) {
            armor.removeModifier(MARK_ARMOR_UUID);
            armor.addTransientModifier(new AttributeModifier(MARK_ARMOR_UUID,
                    "goetyarcana:void_mark_armor", -0.10D * stacks,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if (damage != null) {
            damage.removeModifier(MARK_DAMAGE_UUID);
            damage.addTransientModifier(new AttributeModifier(MARK_DAMAGE_UUID,
                    "goetyarcana:void_mark_damage", -0.15D * stacks,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void clearMarkModifiers(LivingEntity target) {
        target.getPersistentData().remove(MARK_APPLIED_TAG);
        var armor = target.getAttribute(Attributes.ARMOR);
        var damage = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (armor != null) {
            armor.removeModifier(MARK_ARMOR_UUID);
        }
        if (damage != null) {
            damage.removeModifier(MARK_DAMAGE_UUID);
        }
    }

    // ---------------------------------------------------------------- 击杀：虚空飞弹（技能 2）

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || player.level().isClientSide() || event.getEntity() == player) {
            return;
        }
        if (!isFullSet(player) || !(event.getEntity().level() instanceof ServerLevel server)) {
            return;
        }
        int count = 2 + player.getRandom().nextInt(4);
        var targets = server.getEntitiesOfClass(LivingEntity.class,
                event.getEntity().getBoundingBox().inflate(12.0D),
                e -> e != player && e != event.getEntity() && e.isAlive());
        java.util.Collections.shuffle(targets, new java.util.Random(player.getRandom().nextLong()));
        for (int i = 0; i < Math.min(count, targets.size()); i++) {
            LivingEntity target = targets.get(i);
            float damage = target.getMaxHealth() * 0.42F;
            var source = ModDamageTypes.source(server, ModDamageTypes.VOID_BLAST, player, player);
            target.invulnerableTime = 0;
            target.hurt(source, damage);
            server.sendParticles(com.goetyarcana.registry.ModParticles.VOID_FLAME.get(),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    12, 0.4D, 0.5D, 0.4D, 0.04D);
        }
        server.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);
    }
}
