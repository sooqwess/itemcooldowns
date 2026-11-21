package com.sooqwess.itemcooldowns;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thread-safe per-player cooldown storage.
 *
 * <p>Cooldowns are isolated by player UUID and material: one player's cooldown
 * never blocks another player. All operations are synchronized so the same
 * player cannot pass a check/start gap on multi-region servers (Folia).</p>
 */
public final class CooldownTracker {

    private final Map<UUID, Map<Material, Entry>> cooldowns = new HashMap<>();

    /** Immutable snapshot of one active cooldown, used by GUI / scoreboard / placeholders. */
    public static final class ActiveCooldown {
        private final Material material;
        private final long remainingMillis;
        private final long totalMillis;
        private final double progress;

        public ActiveCooldown(Material material, long remainingMillis, long totalMillis) {
            this.material = material;
            this.remainingMillis = Math.max(0L, remainingMillis);
            this.totalMillis = Math.max(1L, totalMillis);
            this.progress = Math.min(1.0, Math.max(0.0, 1.0 - (double) this.remainingMillis / (double) this.totalMillis));
        }

        public Material getMaterial() {
            return material;
        }

        public long getRemainingMillis() {
            return remainingMillis;
        }

        public long getRemainingSeconds() {
            return (long) Math.ceil(remainingMillis / 1000.0);
        }

        public long getTotalMillis() {
            return totalMillis;
        }

        public double getProgress() {
            return progress;
        }
    }

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
        map.put(material, new Entry(now + seconds * 1000L, seconds * 1000L));
        if (overlay) {
            player.setCooldown(material, seconds * 20);
        }
        return 0L;
    }

    /** Remaining time for one material, or 0 when ready. Prunes expired entries. */
    public synchronized long remaining(Player player, Material material) {
        Map<Material, Entry> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            return 0L;
        }
        Entry entry = map.get(material);
        if (entry == null) {
            return 0L;
        }
        long remaining = entry.expiry - System.currentTimeMillis();
        if (remaining <= 0L) {
            map.remove(material);
            if (map.isEmpty()) {
                cooldowns.remove(player.getUniqueId());
            }
            return 0L;
        }
        return remaining;
    }

    /** Snapshot of all active cooldowns for a player, sorted by remaining time (longest first). */
    public synchronized List<ActiveCooldown> active(Player player) {
        Map<Material, Entry> map = cooldowns.get(player.getUniqueId());
        if (map == null || map.isEmpty()) {
            return Collections.emptyList();
        }
        long now = System.currentTimeMillis();
        List<ActiveCooldown> result = new ArrayList<>();
        List<Material> expired = new ArrayList<>();
        for (Map.Entry<Material, Entry> e : map.entrySet()) {
            long remaining = e.getValue().expiry - now;
            if (remaining <= 0L) {
                expired.add(e.getKey());
            } else {
                result.add(new ActiveCooldown(e.getKey(), remaining, e.getValue().totalMillis));
            }
        }
        for (Material m : expired) {
            map.remove(m);
        }
        if (map.isEmpty()) {
            cooldowns.remove(player.getUniqueId());
        }
        result.sort((a, b) -> Long.compare(b.getRemainingMillis(), a.getRemainingMillis()));
        return result;
    }

    /** Nearest (longest remaining) active cooldown, or null when everything is ready. */
    public synchronized ActiveCooldown nearest(Player player) {
        List<ActiveCooldown> list = active(player);
        return list.isEmpty() ? null : list.get(0);
    }

    public synchronized int activeCount(Player player) {
        return active(player).size();
    }

    /** Clears one material for one player. Returns true when something was cleared. */
    public synchronized boolean clear(Player player, Material material) {
        Map<Material, Entry> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            return false;
        }
        boolean removed = map.remove(material) != null;
        if (removed) player.setCooldown(material, 0);
        if (map.isEmpty()) {
            cooldowns.remove(player.getUniqueId());
        }
        return removed;
    }

    public synchronized void clear(Player player) {
        Map<Material, Entry> removed = cooldowns.remove(player.getUniqueId());
        if (removed != null) for (Material material : removed.keySet()) player.setCooldown(material, 0);
    }

    public synchronized void clear() {
        cooldowns.clear();
    }

    private static final class Entry {
        private final long expiry;
        private final long totalMillis;

        private Entry(long expiry, long totalMillis) {
            this.expiry = expiry;
            this.totalMillis = totalMillis;
        }
    }
}
