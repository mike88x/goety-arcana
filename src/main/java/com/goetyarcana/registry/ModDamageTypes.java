package com.goetyarcana.registry;

import com.goetyarcana.GoetyArcanaMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * 自定义伤害类型。
 *
 * <p>{@code void_wave}：剑气的百分比伤害，带 {@code bypasses_armor}，
 * 但<b>不带</b> {@code bypasses_invulnerability}，所以仍受 Boss 常规减伤影响。</p>
 *
 * <p>{@code execution}：处决结算，带 {@code bypasses_armor} 与
 * {@code bypasses_invulnerability}，可以绕过 Goety 的 Boss 伤害上限。</p>
 */
public final class ModDamageTypes {

    public static final ResourceKey<DamageType> VOID_WAVE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "void_wave"));

    public static final ResourceKey<DamageType> EXECUTION = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "execution"));

    public static final ResourceKey<DamageType> SOUL = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new net.minecraft.resources.ResourceLocation(GoetyArcanaMod.MOD_ID, "soul"));

    public static final ResourceKey<DamageType> VOID_AMULET = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "void_amulet"));

    public static final ResourceKey<DamageType> VOID_BLAST = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "void_blast"));

    public static final ResourceKey<DamageType> VINE_POISON = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(GoetyArcanaMod.MOD_ID, "vine_poison"));

    public static DamageSource source(Level level, ResourceKey<DamageType> key,
                                      @Nullable Entity directEntity, @Nullable Entity owner) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key);
        return new DamageSource(holder, directEntity, owner);
    }

    private ModDamageTypes() {
    }
}
