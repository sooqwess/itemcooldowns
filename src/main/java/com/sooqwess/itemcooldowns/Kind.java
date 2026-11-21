package com.sooqwess.itemcooldowns;

/**
 * How a rule is triggered.
 * <ul>
 *   <li>ATTACK_ONLY — melee hit with the item in the main hand (mace).</li>
 *   <li>ATTACK_AND_USE — melee hit and right-click use (spear, trident).</li>
 *   <li>BLOCK_USE — right-click use only (end crystal, respawn anchor, pearl, shield, wind charge).</li>
 *   <li>SHOOT_ONLY — bow shot via PlayerShootBowEvent.</li>
 *   <li>RESURRECT_ONLY — totem consumption via PlayerResurrectEvent.</li>
 * </ul>
 */
public enum Kind {
    ATTACK_ONLY,
    ATTACK_AND_USE,
    BLOCK_USE,
    SHOOT_ONLY,
    RESURRECT_ONLY;

    public boolean isAttackGated() {
        return this == ATTACK_ONLY || this == ATTACK_AND_USE;
    }

    public boolean isUseGated() {
        return this == ATTACK_AND_USE || this == BLOCK_USE;
    }
}
