package com.goetyarcana.network;

import com.goetyarcana.client.SoulHeartsCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 同步玩家的灵魂心数量（护盾），客户端 HUD 用来画青蓝色的心。
 */
public class SoulHeartsPacket {

    private final int entityId;
    private final int hearts;

    public SoulHeartsPacket(int entityId, int hearts) {
        this.entityId = entityId;
        this.hearts = hearts;
    }

    public static void encode(SoulHeartsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.hearts);
    }

    public static SoulHeartsPacket decode(FriendlyByteBuf buffer) {
        return new SoulHeartsPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SoulHeartsPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SoulHeartsCache.set(packet.entityId, packet.hearts)));
        ctx.setPacketHandled(true);
    }
}
