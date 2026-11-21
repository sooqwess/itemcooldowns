package com.sooqwess.itemcooldowns.hook;

import com.sooqwess.itemcooldowns.Config;
import com.sooqwess.itemcooldowns.CooldownTracker;
import com.sooqwess.itemcooldowns.ItemCooldowns;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion. Only loaded by {@link PapiHook} when PlaceholderAPI exists.
 *
 * <p>Placeholders:
 * <ul>
 *   <li>{@code %itemcooldowns_active%} — number of active cooldowns</li>
 *   <li>{@code %itemcooldowns_nearest%} — nearest item key or "ready"</li>
 *   <li>{@code %itemcooldowns_nearest_seconds%} — nearest remaining seconds</li>
 *   <li>{@code %itemcooldowns_<item>_seconds%} — remaining seconds for an item key</li>
 *   <li>{@code %itemcooldowns_<item>_ready%} — true/false</li>
 * </ul>
 */
public final class PapiExpansion extends PlaceholderExpansion {

    private final ItemCooldowns plugin;

    PapiExpansion(ItemCooldowns plugin) {
        this.plugin = plugin;
    }

    void registerExpansion() {
        register();
    }

    @Override
    public String getIdentifier() {
        return "itemcooldowns";
    }

    @Override
    public String getAuthor() {
        return "Sooqwess";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null || identifier == null) {
            return "";
        }
        String id = identifier.toLowerCase(java.util.Locale.ROOT);
        if ("active".equals(id)) {
            return String.valueOf(plugin.getTracker().activeCount(player));
        }
        if ("nearest".equals(id)) {
            CooldownTracker.ActiveCooldown nearest = plugin.getTracker().nearest(player);
            if (nearest == null) {
                return "ready";
            }
            Config.ItemRule rule = plugin.getConfigManager().ruleFor(nearest.getMaterial());
            return rule != null ? rule.getKey() : nearest.getMaterial().name().toLowerCase(java.util.Locale.ROOT);
        }
        if ("nearest_seconds".equals(id)) {
            CooldownTracker.ActiveCooldown nearest = plugin.getTracker().nearest(player);
            return nearest == null ? "0" : String.valueOf(nearest.getRemainingSeconds());
        }
        if (id.endsWith("_seconds")) {
            Config.ItemRule rule = plugin.getConfigManager().ruleForKey(id.substring(0, id.length() - "_seconds".length()));
            Material material = firstMaterial(rule);
            if (material == null) {
                return "";
            }
            return String.valueOf((long) Math.ceil(remaining(player, rule) / 1000.0));
        }
        if (id.endsWith("_ready")) {
            Config.ItemRule rule = plugin.getConfigManager().ruleForKey(id.substring(0, id.length() - "_ready".length()));
            Material material = firstMaterial(rule);
            if (material == null) {
                return "";
            }
            return remaining(player, rule) <= 0L ? "true" : "false";
        }
        return "";
    }

    private long remaining(Player player, Config.ItemRule rule) {
        return rule.getMaterials().stream().mapToLong(m -> plugin.getTracker().remaining(player, m)).max().orElse(0L);
    }

    private Material firstMaterial(Config.ItemRule rule) {
        if (rule == null || rule.getMaterials().isEmpty()) {
            return null;
        }
        return rule.getMaterials().iterator().next();
    }
}
