package com.sooqwess.itemcooldowns;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class Lang {

    private final ItemCooldowns plugin;
    private final YamlConfiguration messages;

    public Lang(ItemCooldowns plugin, String locale) {
        this.plugin = plugin;
        File folder = new File(plugin.getDataFolder(), "lang");
        folder.mkdirs();
        for (String bundled : new String[]{"en", "ru"}) {
            String path = "lang/messages-" + bundled + ".yml";
            if (plugin.getResource(path) != null && !new File(folder, "messages-" + bundled + ".yml").exists()) {
                plugin.saveResource(path, false);
            }
        }
        YamlConfiguration config = new YamlConfiguration();
        File file = new File(folder, "messages-" + locale + ".yml");
        if (file.exists()) {
            try {
                config.load(file);
            } catch (Exception ignored) {
            }
        }
        YamlConfiguration fallback = null;
        for (String candidate : new String[]{locale, "en", "ru"}) {
            InputStream stream = plugin.getResource("lang/messages-" + candidate + ".yml");
            if (stream == null) {
                continue;
            }
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (fallback != null) {
                bundled.setDefaults(fallback);
            }
            fallback = bundled;
        }
        if (fallback != null) {
            config.setDefaults(fallback);
        }
        this.messages = config;
    }

    public String get(String path) {
        return ChatColor.translateAlternateColorCodes('&', messages.getString(path, path));
    }

    public String getPrefix() {
        return get("prefix");
    }

    public void sendBlocked(Player player, Config.ItemRule rule, long remainingMillis) {
        if (!plugin.getConfigManager().isMessagesEnabled()) {
            return;
        }
        long seconds = (long) Math.ceil(remainingMillis / 1000.0);
        player.sendMessage(getPrefix() + get("cooldown.blocked")
                .replace("{item}", get("items." + rule.getKey()))
                .replace("{seconds}", String.valueOf(seconds)));
    }

    public void sendStarted(Player player, Config.ItemRule rule, int seconds) {
        if (!plugin.getConfigManager().isMessagesEnabled() || !plugin.getConfigManager().isNotifyStart()) {
            return;
        }
        player.sendMessage(getPrefix() + get("cooldown.started")
                .replace("{item}", get("items." + rule.getKey()))
                .replace("{seconds}", String.valueOf(seconds)));
    }
}
