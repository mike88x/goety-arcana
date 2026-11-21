package com.goetyarcana.network;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.client.ExecutionDeathAnimation;
import com.goetyarcana.combat.ScytheCombat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * 极简网络通道。
 *
 * <ul>
 *   <li>C2S：左键空气时把 360° 横扫请求发到服务端；</li>
 *   <li>S2C：被处决致死的怪物，通知附近客户端播放专属死亡动画。</li>
 * </ul>
 */
public final class ModNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ScytheSwingPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ScytheSwingPacket::encode)
                .decoder(ScytheSwingPacket::decode)
                .consumerMainThread(ScytheSwingPacket::handle)
                .add();

        CHANNEL.messageBuilder(ExecutionDeathPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ExecutionDeathPacket::encode)
                .decoder(ExecutionDeathPacket::decode)
                .consumerMainThread(ExecutionDeathPacket::handle)
                .add();

        CHANNEL.messageBuilder(SoulHeartsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SoulHeartsPacket::encode).decoder(SoulHeartsPacket::decode)
                .consumerMainThread(SoulHeartsPacket::handle).add();
        CHANNEL.messageBuilder(VoidCursePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(VoidCursePacket::encode)
                .decoder(VoidCursePacket::decode)
                .consumerMainThread(VoidCursePacket::handle)
                .add();
    }

    /** 把「某只生物中了砰砰诅咒」告诉附近客户端（BOSS 上效果会被即时清掉，只能这样同步）。 */
    public static void sendSoulHearts(net.minecraft.server.level.ServerLevel level,
                                      net.minecraft.world.entity.player.Player player, int hearts) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SoulHeartsPacket(player.getId(), hearts));
    }

    public static void sendVoidCurse(ServerLevel level, Entity target, int durationTicks) {
        VoidCursePacket packet = new VoidCursePacket(target.getId(), durationTicks);
        double range = 96.0D * 96.0D;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(target) <= range) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

    public static void sendScytheSwing() {
        CHANNEL.sendToServer(new ScytheSwingPacket());
    }

    /**
     * 把「某只怪物被处决致死」告诉看得见它的玩家。
     *
     * <p>只发给 96 格内的玩家，避免全服广播；客户端收到后会给这只怪物套上
     * 20 tick 的虚空湮灭动画（{@link ExecutionDeathAnimation}）。</p>
     */
    public static void sendExecutionDeath(ServerLevel level, Entity target) {
        ExecutionDeathPacket packet = new ExecutionDeathPacket(target.getId());
        double range = 96.0D * 96.0D;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(target) <= range) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

    public record ScytheSwingPacket() {

        public static void encode(ScytheSwingPacket packet, FriendlyByteBuf buffer) {
        }

        public static ScytheSwingPacket decode(FriendlyByteBuf buffer) {
            return new ScytheSwingPacket();
        }

        public static void handle(ScytheSwingPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender != null) {
                    ScytheCombat.trySweep(sender);
                }
            });
            context.setPacketHandled(true);
        }
    }

    /** 处决死亡动画通知；客户端在 {@link ExecutionDeathAnimation} 里消费。 */
    public record ExecutionDeathPacket(int entityId) {

        public static void encode(ExecutionDeathPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.entityId());
        }

        public static ExecutionDeathPacket decode(FriendlyByteBuf buffer) {
            return new ExecutionDeathPacket(buffer.readVarInt());
        }

        public static void handle(ExecutionDeathPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ExecutionDeathAnimation.onPacket(packet.entityId())));
            context.setPacketHandled(true);
        }
    }

    /** 砰砰诅咒的客户端标记。 */
    public record VoidCursePacket(int entityId, int durationTicks) {

        public static void encode(VoidCursePacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.entityId());
            buffer.writeVarInt(packet.durationTicks());
        }

        public static VoidCursePacket decode(FriendlyByteBuf buffer) {
            return new VoidCursePacket(buffer.readVarInt(), buffer.readVarInt());
        }

        public static void handle(VoidCursePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.goetyarcana.client.VoidCurseCache.accept(
                            packet.entityId(), packet.durationTicks())));
            context.setPacketHandled(true);
        }
    }

    private ModNetwork() {
    }
}
