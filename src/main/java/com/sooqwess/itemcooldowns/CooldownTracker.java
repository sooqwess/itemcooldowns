package com.sooqwess.itemcooldowns;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownTracker {

    private static final long SAME_ATTACK_WINDOW_MS = 50;

    private final Map<UUID, Map<Material, Entry>> cooldowns = new ConcurrentHashMap<>();

    public long remaining(Player player, Material material) {
        Map<Material, Entry> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            return 0L;
        }
        Entry entry = map.get(material);
        if (entry == null) {
            return 0L;
        }
        long now = System.currentTimeMillis();
        if (now - entry.startMillis <= SAME_ATTACK_WINDOW_MS) {
            return 0L;
        }
        long remaining = entry.expiry - now;
        if (remaining <= 0L) {
            map.remove(material);
            if (map.isEmpty()) {
                cooldowns.remove(player.getUniqueId());
            }
            return 0L;
        }
        return remaining;
    }

    public void start(Player player, Material material, int seconds, boolean overlay) {
        long now = System.currentTimeMillis();
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(material, new Entry(now + seconds * 1000L, now));
        if (overlay) {
            player.setCooldown(material, seconds * 20);
        }
    }

    public void clear(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    public void clear() {
        cooldowns.clear();
    }

    private static final class Entry {

        private final long expiry;
        private final long startMillis;

        private Entry(long expiry, long startMillis) {
            this.expiry = expiry;
            this.startMillis = startMillis;
        }
    }
}
