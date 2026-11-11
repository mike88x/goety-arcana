package com.goetyarcana.client;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.VoidScytheBalance;
import com.goetyarcana.registry.ModEntities;
import com.goetyarcana.registry.ModItems;
import com.goetyarcana.registry.ModParticles;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端注册：剑气渲染器 + 物品模型/蓄力属性。
 */
@Mod.EventBusSubscriber(modid = GoetyArcanaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SOUL_BURST.get(), SoulBurstRenderer::new);
        event.registerEntityRenderer(ModEntities.SCYTHE_WAVE.get(), ScytheWaveRenderer::new);
        event.registerEntityRenderer(ModEntities.EXECUTION_SLASH.get(), ExecutionSlashRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.VOID_FLAME.get(), VoidFlameParticle.Provider::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.VOID_SCYTHE.get(),
                new ResourceLocation(GoetyArcanaMod.MOD_ID, "charge"),
                (stack, level, entity, seed) -> chargeOf(stack, entity)));
    }

    /**
     * 物品模型 predicate 使用：
     * 未蓄力 = 0；蓄力中 = 0.02~0.98；满蓄 = 1.0。
     *
     * <p>模型 JSON 用 0.01 / 0.34 / 0.67 / 0.99 四个阈值切换四档贴图，
     * 让刀片逐步变亮，最后在满蓄时切到发光版本。</p>
     */
    private static float chargeOf(ItemStack stack, LivingEntity entity) {
        if (entity == null || !entity.isUsingItem()) {
            return 0.0F;
        }
        ItemStack using = entity.getUseItem();
        if (!ItemStack.isSameItemSameTags(stack, using)) {
            return 0.0F;
        }

        int used = entity.getTicksUsingItem();
        if (used < VoidScytheBalance.MIN_CHARGE_TICKS) {
            return 0.0F;
        }

        float ratio = (used - VoidScytheBalance.MIN_CHARGE_TICKS)
                / (float) (VoidScytheBalance.FULL_CHARGE_TICKS - VoidScytheBalance.MIN_CHARGE_TICKS);
        return Mth.clamp(0.02F + 0.98F * ratio, 0.02F, 1.0F);
    }
}
