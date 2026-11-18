package com.goetyarcana.event;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.entity.VoidCreeper;
import com.goetyarcana.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 末地城 / 终末永狱附近额外补充虚空苦力怕。
 *
 * <p>生物群系刷怪权重无法表达「只在结构附近多刷」，所以每 100 tick 检查一次：
 * 玩家在末地、附近有结构、且 48 格内没有虚空苦力怕时，有概率在结构地面上补一只。</p>
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VoidCreeperSpawnBoost {

    private static final int INTERVAL = 100;
    private static final float CHANCE = 0.45F;

    private VoidCreeperSpawnBoost() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel server)) {
            return;
        }
        if (server.dimension() != Level.END || server.getGameTime() % INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : server.players()) {
            if (player.isSpectator() || server.random.nextFloat() > CHANCE) {
                continue;
            }
            BlockPos origin = player.blockPosition();
            if (!VoidCreeper.nearEndStructure(server, origin)) {
                continue;
            }
            AABB area = player.getBoundingBox().inflate(48.0D);
            if (!server.getEntitiesOfClass(VoidCreeper.class, area).isEmpty()) {
                continue;
            }
            for (int attempt = 0; attempt < 8; attempt++) {
                int dx = server.random.nextInt(33) - 16;
                int dz = server.random.nextInt(33) - 16;
                BlockPos pos = origin.offset(dx, 0, dz);
                BlockPos surface = server.getHeightmapPos(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
                if (!server.isEmptyBlock(surface.above()) || !server.isEmptyBlock(surface.above(2))) {
                    continue;
                }
                VoidCreeper creeper = ModEntities.VOID_CREEPER.get().create(server);
                if (creeper == null) {
                    break;
                }
                creeper.moveTo(surface.getX() + 0.5D, surface.getY(), surface.getZ() + 0.5D,
                        server.random.nextFloat() * 360.0F, 0.0F);
                creeper.finalizeSpawn(server, server.getCurrentDifficultyAt(surface),
                        MobSpawnType.EVENT, null, null);
                server.addFreshEntity(creeper);
                break;
            }
        }
    }
}
