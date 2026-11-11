package com.goetyarcana.network;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.combat.ScytheCombat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * 极简网络通道：只用于「左键空气」时把 360° 横扫请求发到服务端。
 *
 * <p>左键方块和左键实体会在服务端直接触发，不需要这个包；
 * 只有空中挥刀在原版里没有服务端事件，所以补一个空包。</p>
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
    }

    public static void sendScytheSwing() {
        CHANNEL.sendToServer(new ScytheSwingPacket());
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

    private ModNetwork() {
    }
}
