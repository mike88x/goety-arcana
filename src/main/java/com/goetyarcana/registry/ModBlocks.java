package com.goetyarcana.registry;

import com.goetyarcana.GoetyArcanaMod;
import com.goetyarcana.block.VoidCreeperHeadBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, GoetyArcanaMod.MOD_ID);

    /** 虚空苦力怕头颅 */
    public static final RegistryObject<Block> VOID_CREEPER_HEAD =
            BLOCKS.register("void_creeper_head", () -> new VoidCreeperHeadBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(1.0F)
                            .sound(SoundType.WOOL)
                            .noOcclusion()));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
