package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.VoidScytheBalance;
import com.goetyarcana.registry.ModParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 「处决死亡动画」。
 *
 * <p>被处决 debuff 杀死的怪物不再只是原版那样侧倒下去，而是：</p>
 * <ul>
 *   <li>身体被虚空抬起、旋转、逐渐压扁缩小，最后沉进地面；</li>
 *   <li>周围不断有虚空火从外圈向身体收束，夹杂上升的灵魂粒子；</li>
 *   <li>整个动画持续 {@link VoidScytheBalance#EXECUTION_DEATH_TICKS} tick，
 *       与原版尸体存在的 20 tick 完全一致。</li>
 * </ul>
 *
 * <p>实现上由服务端在结算处决后发 {@code ExecutionDeathPacket}，客户端把实体 ID
 * 记进 {@link #ACTIVE}，再在 {@link RenderLivingEvent.Pre} 里接管这次渲染：</p>
 * <ol>
 *   <li>取消原版渲染，在外部 PoseStack 上叠加死亡姿态；</li>
 *   <li>临时把 {@code deathTime} 清零再调用渲染器，跳过原版“侧倒”动画；</li>
 *   <li>用 {@code finally} 恢复 {@code deathTime} 并弹出 PoseStack，
 *       即使渲染被其它模组取消也不会把矩阵栈弄乱。</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ExecutionDeathAnimation {

    /** 还没开始/正在播放的死亡动画；key = 实体 ID。 */
    private static final Map<Integer, Entry> ACTIVE = new HashMap<>();

    /** 正在被本类重新渲染的实体，防止 Pre 事件无限递归。 */
    private static final Set<Integer> RE_RENDERING = new HashSet<>();

    /** 客户端 tick 计数，进度用它计算（不受单机暂停影响）。 */
    private static long clientTick;

    private ExecutionDeathAnimation() {
    }

    /** 网络包入口：服务端说「这只怪物被处决了」。 */
    public static void onPacket(int entityId) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        ACTIVE.putIfAbsent(entityId, new Entry(clientTick));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        clientTick++;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ACTIVE.clear();
            return;
        }

        Iterator<Map.Entry<Integer, Entry>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Entry> mapEntry = iterator.next();
            Entity entity = minecraft.level.getEntity(mapEntry.getKey());
            Entry entry = mapEntry.getValue();

            if (!(entity instanceof LivingEntity living) || living.isRemoved()) {
                iterator.remove();
                continue;
            }

            if (entry.startTick < 0) {
                if (!living.isDeadOrDying()) {
                    // 包比死亡状态早到一帧是正常的；但等太久就当它没死。
                    if (clientTick - entry.packetTick > 40) {
                        iterator.remove();
                    }
                    continue;
                }
                entry.startTick = clientTick;
            }

            float progress = (clientTick - entry.startTick)
                    / (float) VoidScytheBalance.EXECUTION_DEATH_TICKS;
            if (progress >= 1.5F) {
                iterator.remove();
                continue;
            }
            if (progress < 1.0F) {
                spawnVoidParticles(living, progress);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        int id = entity.getId();

        // 本类自己发起的第二次 render：让渲染正常继续，姿态已经在外面套好了。
        if (RE_RENDERING.contains(id)) {
            return;
        }

        Entry entry = ACTIVE.get(id);
        if (entry == null || entry.startTick < 0 || !entity.isDeadOrDying()) {
            return;
        }

        float progress = (clientTick - entry.startTick + event.getPartialTick())
                / (float) VoidScytheBalance.EXECUTION_DEATH_TICKS;
        if (progress >= 1.0F) {
            return;
        }

        event.setCanceled(true);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        applyDeathPose(poseStack, entity, progress);

        // 原版 setupRotations 会用 deathTime 把尸体侧倒 90°；临时清零跳过它，
        // 换成上面这套“被虚空吸走”的姿态。
        int deathTime = entity.deathTime;
        entity.deathTime = 0;
        RE_RENDERING.add(id);
        try {
            renderWithPose(event, entity);
        } finally {
            RE_RENDERING.remove(id);
            entity.deathTime = deathTime;
            poseStack.popPose();
        }
    }

    /**
     * 重新调用实体渲染器；通过 {@link #RE_RENDERING} 让嵌套的 Pre 事件放行。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void renderWithPose(RenderLivingEvent.Pre<?, ?> event, LivingEntity entity) {
        LivingEntityRenderer renderer = event.getRenderer();
        float bodyYaw = Mth.lerp(event.getPartialTick(), entity.yRotO, entity.getYRot());
        renderer.render(entity, bodyYaw, event.getPartialTick(), event.getPoseStack(),
                event.getMultiBufferSource(), event.getPackedLight());
    }

    /**
     * 死亡姿态：先被抬起、旋转，再被压扁、缩小并沉入地面。
     */
    private static void applyDeathPose(PoseStack poseStack, LivingEntity entity, float progress) {
        float p = Mth.clamp(progress, 0.0F, 1.0F);
        float ease = p * p * (3.0F - 2.0F * p);
        float arc = Mth.sin(p * Mth.PI);

        float lift = 0.55F * arc;
        float sink = -0.55F * ease * ease;
        poseStack.translate(0.0F, lift + sink, 0.0F);

        // 绕世界 Y 轴自转一整圈。
        poseStack.mulPose(Axis.YP.rotationDegrees(360.0F * ease));

        // 沿身体“右方”侧倾，无论朝向如何都是往侧面倒，而不是固定绕世界 Z 轴。
        float yawRad = entity.getYRot() * Mth.DEG_TO_RAD;
        poseStack.mulPose(new Quaternionf().rotationAxis(
                14.0F * arc * Mth.DEG_TO_RAD,
                Mth.cos(yawRad), 0.0F, Mth.sin(yawRad)));

        float horizontal = 1.0F - 0.90F * ease;
        float vertical = 1.0F - 0.55F * ease;
        poseStack.scale(horizontal, vertical, horizontal);
    }

    private static void spawnVoidParticles(LivingEntity entity, float progress) {
        Level level = entity.level();
        double centerX = entity.getX();
        double centerZ = entity.getZ();
        double centerY = entity.getY() + entity.getBbHeight() * 0.5D;
        double height = entity.getBbHeight();
        float remaining = 1.0F - Mth.clamp(progress, 0.0F, 1.0F);
        double radius = 0.25D + 0.95D * remaining;

        for (int i = 0; i < 5; i++) {
            double angle = entity.tickCount * 0.55D + i * (Math.PI * 2.0D / 5.0D)
                    + entity.getRandom().nextDouble() * 0.5D;
            double r = radius * (0.7D + entity.getRandom().nextDouble() * 0.5D);
            double x = centerX + Math.cos(angle) * r;
            double z = centerZ + Math.sin(angle) * r;
            double y = centerY + (entity.getRandom().nextDouble() - 0.5D)
                    * height * (0.45D + 0.55D * remaining);
            // 速度指向身体中心：粒子从外圈收束，像被吸进虚空。
            level.addParticle(ModParticles.VOID_FLAME.get(), x, y, z,
                    -Math.cos(angle) * 0.10D,
                    0.02D + 0.02D * remaining,
                    -Math.sin(angle) * 0.10D);
        }

        if (entity.tickCount % 2 == 0) {
            level.addParticle(ParticleTypes.SOUL,
                    centerX + (entity.getRandom().nextDouble() - 0.5D) * 0.7D,
                    entity.getY() + height * (0.25D + entity.getRandom().nextDouble() * 0.6D),
                    centerZ + (entity.getRandom().nextDouble() - 0.5D) * 0.7D,
                    0.0D, 0.045D, 0.0D);
        }
    }

    private static final class Entry {

        private final long packetTick;
        private long startTick = -1L;

        private Entry(long packetTick) {
            this.packetTick = packetTick;
        }
    }
}
