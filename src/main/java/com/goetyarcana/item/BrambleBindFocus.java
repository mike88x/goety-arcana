package com.goetyarcana.item;

import com.Polarice3.Goety.common.items.magic.MagicFocus;
import com.goetyarcana.spell.BrambleBindSpell;

/**
 * 荆棘藤缚（荒野系）。
 *
 * <p>射出一颗种子；种子插在地上后会在 3×3 范围内找敌对生物，
 * 找到就立刻长出藤蔓缠住它们（详见 {@code BrambleSeedEntity}）。</p>
 */
public class BrambleBindFocus extends MagicFocus {

    public BrambleBindFocus() {
        super(new BrambleBindSpell());
    }
}
