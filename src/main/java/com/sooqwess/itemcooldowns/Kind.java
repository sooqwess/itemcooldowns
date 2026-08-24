package com.sooqwess.itemcooldowns;

public enum Kind {
    ATTACK_ONLY,
    ATTACK_AND_USE,
    BLOCK_USE;

    public boolean isAttackGated() {
        return this == ATTACK_ONLY || this == ATTACK_AND_USE;
    }

    public boolean isUseGated() {
        return this == ATTACK_AND_USE || this == BLOCK_USE;
    }
}
