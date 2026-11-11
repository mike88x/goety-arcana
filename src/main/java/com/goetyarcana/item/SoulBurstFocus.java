package com.goetyarcana.item;

import com.Polarice3.Goety.common.items.magic.MagicFocus;
import com.goetyarcana.spell.SoulBurstSpell;

/**
 * 裂魂聚晶物品。
 *
 * {@link MagicFocus} 是 Goety 官方聚晶基类（实现 {@code IFocus}）：
 * 法杖/聚晶环会通过它拿到 {@link SoulBurstSpell}，
 * 灵魂能量消耗、附魔台可附魔判定、tooltip 也都由它处理。
 */
public class SoulBurstFocus extends MagicFocus {

    public SoulBurstFocus() {
        super(new SoulBurstSpell());
    }
}
