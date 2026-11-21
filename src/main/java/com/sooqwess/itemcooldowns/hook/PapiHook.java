package com.sooqwess.itemcooldowns.hook;

import com.sooqwess.itemcooldowns.ItemCooldowns;

/**
 * Loads the optional PlaceholderAPI expansion.
 *
 * <p>This class deliberately does <b>not</b> extend or import any PlaceholderAPI type:
 * touching {@link PapiExpansion} would otherwise throw {@link NoClassDefFoundError}
 * on servers where PlaceholderAPI is not installed. The expansion class is only
 * loaded after a successful plugin lookup, inside a {@code Throwable} guard.</p>
 */
public final class PapiHook {

    private PapiHook() {
    }

    public static void register(ItemCooldowns plugin) {
        try {
            if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
                plugin.getLogger().info("PlaceholderAPI not found: placeholders disabled.");
                return;
            }
            PapiExpansion expansion = new PapiExpansion(plugin);
            expansion.registerExpansion();
            plugin.getLogger().info("PlaceholderAPI hook registered (%itemcooldowns_*%).");
        } catch (Throwable t) {
            plugin.getLogger().warning("PlaceholderAPI hook failed: " + t);
        }
    }
}
