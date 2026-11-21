package com.goetyarcana.event;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.combat.VoidAmulet;
import com.goetyarcana.registry.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 虚空护身符的效果。
 *
 * <ul>
 *   <li>你造成的伤害有 42% 被转成虚空伤害（无视护甲/抗性/附魔）；</li>
 *   <li>跌入虚空时把你拉回地面（同一枚护符有 10 秒冷却）。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VoidAmuletEvents {

    private static final String COOLDOWN_TAG = "goetyarcana.amulet_rescue";
    private static final String SAFE_TAG = "goetyarcana.amulet_safe";
    private static final String NOTE_TAG = "goetyarcana.amulet_noted";
    /** 掉进虚空前最后一个仍在地面以上的位置（飞行时也记），作为兜底。 */
    private static final String LAST_TAG = "goetyarcana.amulet_last_above";
    /** 濒死爆炸的冷却：20 分钟。 */
    private static final String DEATH_CD_TAG = "goetyarcana.amulet_death_cd";
    private static final long DEATH_CD_TICKS = 20L * 60L * 20L;

    private VoidAmuletEvents() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == player || target.level().isClientSide()) {
            return;
        }
        if (event.getSource().is(ModDamageTypes.VOID_AMULET)) {
            return;
        }
        if (!VoidAmulet.isCarried(player)) {
            return;
        }
        float total = event.getAmount();
        if (total <= 0.0F) {
            return;
        }
        float voidPart = total * VoidAmulet.CONVERT_RATIO;
        // 只留下一个伤害数字：原伤害砍掉 42%，剩下这 42% 直接扣血并冒虚空粒子，
        // 不再额外走一次 hurt()（否则战斗假人会同时跳出三个数字）。
        event.setAmount(total - voidPart);
        if (target.isAlive()) {
            float remaining = target.getHealth() - voidPart;
            if (remaining <= 0.0F) {
                var source = ModDamageTypes.source(target.level(),
                        ModDamageTypes.VOID_AMULET, player, player);
                target.setHealth(0.0F);
                target.die(source);
            } else {
                target.setHealth(remaining);
            }
            if (target.level() instanceof ServerLevel server) {
                server.sendParticles(com.goetyarcana.registry.ModParticles.VOID_FLAME.get(),
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        6, 0.3D, 0.3D, 0.3D, 0.02D);
            }
        }
    }

    /**
     * 找一个真正能站稳的位置：优先上次站过的实地，其次在附近一圈圈找最高的实心方块，
     * 都没有就回世界出生点。绝不能再用「当前 x/z 的高度图」——在虚空上方它只有 0。
     */
    private static BlockPos findSafeSpot(ServerPlayer player) {
        var level = player.serverLevel();
        var data = player.getPersistentData();
        BlockPos origin = player.blockPosition();

        // 1) 优先回到「掉下去之前站的那块地」——只要它离得不算太远
        if (data.contains(SAFE_TAG)) {
            BlockPos remembered = BlockPos.of(data.getLong(SAFE_TAG));
            // 记忆点是「脚所在的位置」，所以要检查它脚下方那格是不是实心
            if (remembered.distSqr(origin) <= 512.0D * 512.0D
                    && isStandable(level, remembered.below())) {
                return remembered;
            }
        }
        // 2) 附近找一块能站的地面（避免落到旁边一块孤零零的小岛）
        BlockPos nearby = scanForGroundByBlocks(level, origin, 4, 40, 4);
        if (nearby != null) {
            return nearby;
        }
        // 3) 更大范围找
        BlockPos wide = scanForGroundByBlocks(level, origin, 44, 160, 8);
        if (wide != null) {
            return wide;
        }
        BlockPos farther = scanForGroundByBlocks(level, origin, 168, 512, 16);
        if (farther != null) {
            return farther;
        }
        if (data.contains(LAST_TAG)) {
            BlockPos last = BlockPos.of(data.getLong(LAST_TAG));
            return last;
        }
        return origin;
    }

    /** 直接扫方块（不信任高度图）：每个采样列从世界顶部往下找第一个能站的方块。 */
    private static BlockPos scanForGroundByBlocks(net.minecraft.server.level.ServerLevel level,
                                                  BlockPos origin, int from, int to, int step) {
        int top = level.getMaxBuildHeight() - 2;
        int bottom = level.getMinBuildHeight() + 2;
        for (int radius = from; radius <= to; radius += step) {
            for (int dx = -radius; dx <= radius; dx += step) {
                for (int dz = -radius; dz <= radius; dz += step) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    for (int y = top; y >= bottom; y--) {
                        BlockPos ground = new BlockPos(x, y, z);
                        if (isStandable(level, ground)) {
                            return ground.above();
                        }
                    }
                }
            }
        }
        return null;
    }

    /** 从 from 到 to 一圈圈找最高的实心地面；找不到返回 null。 */
    private static BlockPos scanForGround(net.minecraft.server.level.ServerLevel level,
                                          BlockPos origin, int from, int to) {
        for (int radius = from; radius <= to; radius += 4) {
            for (int dx = -radius; dx <= radius; dx += 4) {
                for (int dz = -radius; dz <= radius; dz += 4) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    if (surface <= level.getMinBuildHeight() + 1) {
                        continue;
                    }
                    BlockPos candidate = new BlockPos(x, surface + 1, z);
                    if (isStandable(level, candidate.below())) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isStandable(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isSolid()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && level.getBlockState(pos.above(2)).getCollisionShape(level, pos.above(2)).isEmpty()
                && !level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LEAVES);
    }

    /**
     * 致命伤害：以玩家为中心炸一次（中心 50 点，向边缘递减），把周围怪物实际受到的伤害总和
     * 的 50% 转成治疗，并给予「虚空恩庇」（减伤随时间从 100% 降到 0%，共 10 分钟）。
     */
    /** 优先级 LOW：先于灵魂套（LOWEST）判定，护符能救就不进灵魂形态。 */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOW)
    public static void onFatalDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getAmount() < player.getHealth()) {
            return;
        }
        if (player.hasEffect(com.goetyarcana.registry.ModEffects.VOID_AEGIS.get())) {
            return;
        }
        if (!VoidAmulet.isCarried(player)) {
            return;
        }
        long now = player.level().getGameTime();
        if (now < player.getPersistentData().getLong(DEATH_CD_TAG)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        // 留 1 点血挡住这一下
        event.setAmount(Math.max(0.0F, player.getHealth() - 1.0F));

        double radius = 6.0D;
        float healed = 0.0F;
        net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(radius);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != player && entity.isAlive())) {
            double distance = Math.sqrt(victim.distanceToSqr(player));
            if (distance > radius) {
                continue;
            }
            double factor = 1.0D - distance / radius;
            float damage = (float) (50.0D * factor);
            if (damage <= 0.0F) {
                continue;
            }
            float before = victim.getHealth();
            victim.hurt(player.damageSources().magic(), damage);
            healed += Math.max(0.0F, before - victim.getHealth());
            // 没被炸死的，往外推一把（越近推得越远）
            if (victim.isAlive()) {
                double dx = victim.getX() - player.getX();
                double dz = victim.getZ() - player.getZ();
                double length = Math.max(0.01D, Math.sqrt(dx * dx + dz * dz));
                double strength = (0.55D + 0.85D * factor) / length;
                victim.knockback(strength, dx * strength, dz * strength);
                victim.hurtMarked = true;
            }
        }
        player.heal(healed * 0.5F);
        player.getPersistentData().putLong(DEATH_CD_TAG, now + DEATH_CD_TICKS);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                com.goetyarcana.registry.ModEffects.VOID_AEGIS.get(),
                com.goetyarcana.effect.VoidAegisEffect.DURATION, 0, false, true, true));

        com.goetyarcana.combat.VoidBlastVisuals.explode(server,
                player.getX(), player.getY() + 0.5D, player.getZ());
        player.displayClientMessage(net.minecraft.network.chat.Component
                .translatable("message.goetyarcana.amulet.aegis"), true);
    }

    /** 虚空恩庇的减伤：比例随剩余时间线性下降。 */
    @SubscribeEvent
    public static void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        var aegis = player.getEffect(com.goetyarcana.registry.ModEffects.VOID_AEGIS.get());
        if (aegis == null) {
            return;
        }
        float remain = aegis.getDuration() / (float) com.goetyarcana.effect.VoidAegisEffect.DURATION;
        float resist = net.minecraft.util.Mth.clamp(remain, 0.0F, 1.0F);
        event.setAmount(event.getAmount() * (1.0F - resist));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        int minY = player.level().getMinBuildHeight();
        // 站在实地上就记一下最后的安全点，掉进虚空后先回这里
        // 站在地上，或悬停在实地上方（不超过 4 格）都算「安全位置」
        boolean grounded = player.onGround();
        if (!grounded) {
            for (int down = 1; down <= 4 && !grounded; down++) {
                grounded = isStandable(player.serverLevel(), player.blockPosition().below(down));
            }
        }
        if (player.getY() > minY + 8) {
            player.getPersistentData().putLong(LAST_TAG, player.blockPosition().asLong());
            if (grounded) {
                player.getPersistentData().putLong(SAFE_TAG, player.blockPosition().asLong());
            }
        }
        if (player.getY() > minY - 8) {
            return;
        }
        if (!VoidAmulet.isCarried(player)) {
            // 掉进虚空但身上没有护身符：给个轻提示，便于排查「到底有没有生效」
            if (!player.getPersistentData().getBoolean(NOTE_TAG)) {
                player.getPersistentData().putBoolean(NOTE_TAG, true);
                player.displayClientMessage(net.minecraft.network.chat.Component
                        .translatable("message.goetyarcana.amulet.missing"), true);
            }
            return;
        }
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        if (now < data.getLong(COOLDOWN_TAG)) {
            return;
        }
        BlockPos safe = findSafeSpot(player);

        player.teleportTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
        player.fallDistance = 0.0F;
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.hurtMarked = true;
        // 救援冷却：2 分钟
        data.putLong(COOLDOWN_TAG, now + 2400L);
        player.displayClientMessage(net.minecraft.network.chat.Component
                .translatable("message.goetyarcana.amulet.rescued"), true);
        if (player.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.6D, 1.0D, 0.6D, 0.12D);
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 12, 0.4D, 0.6D, 0.4D, 0.05D);
            server.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 1.0F, 0.7F);
        }
    }
}
