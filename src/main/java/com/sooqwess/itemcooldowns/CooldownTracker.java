package com.sooqwess.itemcooldowns;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CooldownTracker {

    private final Map<UUID, Map<Material, Entry>> cooldowns = new HashMap<>();

    public long remaining(Player player, Material material) {
        Map<Material, Entry> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            return 0L;
        }
        Entry entry = map.get(material);
        if (entry == null) {
            return 0L;
        }
        if (entry.tick == player.getWorld().getFullTime()) {
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

    public void start(Player player, Material material, int seconds, boolean overlay) {
        long now = System.currentTimeMillis();
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(material, new Entry(now + seconds * 1000L, player.getWorld().getFullTime()));
        if (overlay) {
            player.setCooldown(material, seconds * 20);
        }
    }

    public void clear() {
        cooldowns.clear();
    }

    private static final class Entry {

        private final long expiry;
        private final long tick;

        private Entry(long expiry, long tick) {
            this.expiry = expiry;
            this.tick = tick;
        }
    }
}
