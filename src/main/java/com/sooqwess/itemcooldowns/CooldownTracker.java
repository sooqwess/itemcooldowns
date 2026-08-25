package com.sooqwess.itemcooldowns;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CooldownTracker {

    private final Map<UUID, Map<Material, Entry>> cooldowns = new HashMap<>();

    /**
     * Atomically checks and starts a cooldown. Returns the remaining duration when it was
     * already active, otherwise starts it and returns zero. This prevents duplicate actions
     * from passing a check/start gap on multi-region servers such as Folia.
     */
    public synchronized long startIfReady(Player player, Material material, int seconds, boolean overlay) {
        UUID playerId = player.getUniqueId();
        Map<Material, Entry> map = cooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
        long now = System.currentTimeMillis();
        Entry entry = map.get(material);
        if (entry != null) {
            long remaining = entry.expiry - now;
            if (remaining > 0L) {
                return remaining;
            }
            map.remove(material);
        }
        map.put(material, new Entry(now + seconds * 1000L));
        if (overlay) {
            player.setCooldown(material, seconds * 20);
        }
        return 0L;
    }

    public synchronized void clear(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    public synchronized void clear() {
        cooldowns.clear();
    }

    private static final class Entry {

        private final long expiry;

        private Entry(long expiry) {
            this.expiry = expiry;
        }
    }
}
