package com.sooqwess.itemcooldowns;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Multi-language message system (v2).
 *
 * <p>Bundled locales: en, ru, de, es, fr, pt, zh, ja, ko, uk.
 * {@code locale: auto} picks the player's client language; otherwise the
 * configured locale is used for everyone. Missing keys fall back to English,
 * then Russian. Supports {@code &} colors and {@code &#RRGGBB} hex colors.</p>
 */
public final class Lang {

    private static final String[] BUNDLED = {"en", "ru", "de", "es", "fr", "pt", "zh", "ja", "ko", "uk"};
    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");

    private final ItemCooldowns plugin;
    private final Map<String, YamlConfiguration> cache = new HashMap<>();

    public Lang(ItemCooldowns plugin) {
        this.plugin = plugin;
        File folder = new File(plugin.getDataFolder(), "lang");
        folder.mkdirs();
        for (String bundled : BUNDLED) {
            String path = "lang/messages-" + bundled + ".yml";
            if (plugin.getResource(path) != null && !new File(folder, "messages-" + bundled + ".yml").exists()) {
                plugin.saveResource(path, false);
            }
        }
    }

    /** Resolves the effective locale code for a player (or null for console). */
    public String localeFor(Player player) {
        String configured = plugin.getConfigManager().getLocale();
        if (!"auto".equals(configured)) {
            return normalize(configured);
        }
        if (player != null) {
            try {
                String client = player.getLocale().toLowerCase(Locale.ROOT);
                String code = normalize(client);
                if (isBundled(code)) {
                    return code;
                }
            } catch (Throwable ignored) {
                // Older cores without Player#locale — fall through to English.
            }
        }
        return "en";
    }

    private String normalize(String code) {
        if (code == null) {
            return "en";
        }
        String c = code.toLowerCase(Locale.ROOT).replace('_', '-');
        int dash = c.indexOf('-');
        if (dash > 0) {
            c = c.substring(0, dash);
        }
        return c;
    }

    private boolean isBundled(String code) {
        for (String b : BUNDLED) {
            if (b.equals(code)) {
                return true;
            }
        }
        return false;
    }

    private synchronized YamlConfiguration messages(String locale) {
        String code = normalize(locale);
        if (code == null || code.isEmpty() || "auto".equals(code)) {
            // Console and unset locale: bundled English is the base layer.
            code = "en";
        }
        YamlConfiguration cached = cache.get(code);
        if (cached != null) {
            return cached;
        }
        // Explicit layer merge (en base -> target locale -> on-disk override).
        // setDefaults() is intentionally NOT used: getString(path, def) does not
        // consult the defaults chain on current Paper builds, which used to
        // return raw keys like "commands.reloaded" to the console.
        YamlConfiguration merged = new YamlConfiguration();
        mergeInto(merged, loadBundled("en"));
        if (!"en".equals(code)) {
            mergeInto(merged, loadBundled(code));
        }
        File file = new File(plugin.getDataFolder(), "lang/messages-" + code + ".yml");
        if (file.exists()) {
            try {
                YamlConfiguration disk = new YamlConfiguration();
                disk.load(file);
                mergeInto(merged, disk);
            } catch (Exception ignored) {
            }
        }
        cache.put(code, merged);
        return merged;
    }

    private YamlConfiguration loadBundled(String code) {
        InputStream stream = plugin.getResource("lang/messages-" + code + ".yml");
        if (stream == null) {
            return null;
        }
        try (InputStream in = stream) {
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    /** Copies every leaf value of {@code source} into {@code target}. */
    private static void mergeInto(YamlConfiguration target, YamlConfiguration source) {
        if (source == null) {
            return;
        }
        for (String key : source.getKeys(true)) {
            Object value = source.get(key);
            if (value instanceof org.bukkit.configuration.ConfigurationSection) {
                continue;
            }
            target.set(key, value);
        }
    }

    public synchronized void clearCache() {
        cache.clear();
    }

    public String get(Player player, String path) {
        return color(messages(localeFor(player)).getString(path, path));
    }

    public String get(String path) {
        return color(messages(plugin.getConfigManager().getLocale()).getString(path, path));
    }

    public String getPrefix(Player player) {
        return get(player, "prefix");
    }

    public String getPrefix() {
        return get("prefix");
    }

    public String itemName(Player player, String key) {
        return get(player, "items." + key);
    }

    public void sendBlocked(Player player, Config.ItemRule rule, long remainingMillis) {
        if (!plugin.getConfigManager().isMessagesEnabled()) {
            return;
        }
        long seconds = (long) Math.ceil(remainingMillis / 1000.0);
        player.sendMessage(getPrefix(player) + get(player, "cooldown.blocked")
                .replace("{item}", itemName(player, rule.getKey()))
                .replace("{seconds}", String.valueOf(seconds)));
    }

    public void sendStarted(Player player, Config.ItemRule rule, int seconds) {
        if (!plugin.getConfigManager().isMessagesEnabled() || !plugin.getConfigManager().isNotifyStart()) {
            return;
        }
        player.sendMessage(getPrefix(player) + get(player, "cooldown.started")
                .replace("{item}", itemName(player, rule.getKey()))
                .replace("{seconds}", String.valueOf(seconds)));
    }

    public void sendReady(Player player, Config.ItemRule rule) {
        if (!plugin.getConfigManager().isMessagesEnabled() || !plugin.getConfigManager().isNotifyReady()) {
            return;
        }
        player.sendMessage(getPrefix(player) + get(player, "cooldown.ready")
                .replace("{item}", itemName(player, rule.getKey())));
    }

    /** Translates {@code &} codes and {@code &#RRGGBB} hex colors. */
    public static String color(String text) {
        if (text == null) {
            return "";
        }
        Matcher m = HEX.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char ch : hex.toCharArray()) {
                replacement.append('§').append(ch);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }
}
