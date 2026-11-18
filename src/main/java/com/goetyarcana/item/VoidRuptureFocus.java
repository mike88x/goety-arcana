package com.goetyarcana.item;

import com.Polarice3.Goety.common.items.magic.MagicFocus;
import com.goetyarcana.spell.VoidRuptureSpell;

/**
 * 虚空崩爆聚晶：召唤一只虚空苦力怕仆从。
 *
 * <p>虚空（VOID）系聚晶，图标沿用 Goety 虚空聚晶的宝珠样式。</p>
 */
public class VoidRuptureFocus extends MagicFocus {

    public VoidRuptureFocus() {
        super(new VoidRuptureSpell());
    }
}
