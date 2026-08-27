package com.sooqwess.itemcooldowns;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class PvPManagerHook {

    private Object api;
    private Method canAttack;

    public PvPManagerHook() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PvPManager")) {
            return;
        }
        try {
            Class<?> pvpClass = Class.forName("me.chancesd.pvpmanager.PvPManager");
            Object instance = pvpClass.getMethod("getInstance").invoke(null);
            if (instance != null) {
                Object playerManager = pvpClass.getMethod("getPlayerManager").invoke(instance);
                if (playerManager != null) {
                    canAttack = playerManager.getClass().getMethod("canAttack", Player.class, Player.class);
                    api = playerManager;
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> apiClass = Class.forName("me.Sabledowns.PvPManager.API.API");
            Object legacy = null;
            try {
                legacy = apiClass.getMethod("getAPI").invoke(null);
            } catch (NoSuchMethodException e) {
                legacy = apiClass.getConstructor().newInstance();
            }
            if (legacy != null) {
                canAttack = apiClass.getMethod("shouldAttack", Player.class, Player.class);
                api = legacy;
            }
        } catch (Throwable ignored) {
        }
    }

    public boolean isActive() {
        return api != null && canAttack != null;
    }

    public boolean canAttack(Player attacker, Player defender) {
        if (!isActive()) {
            return true;
        }
        try {
            return Boolean.TRUE.equals(canAttack.invoke(api, attacker, defender));
        } catch (Exception e) {
            return true;
        }
    }
}
