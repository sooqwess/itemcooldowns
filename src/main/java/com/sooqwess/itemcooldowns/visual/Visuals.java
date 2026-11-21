package com.sooqwess.itemcooldowns.visual;

import com.sooqwess.itemcooldowns.Config;
import com.sooqwess.itemcooldowns.CooldownTracker;
import com.sooqwess.itemcooldowns.ItemCooldowns;
import com.sooqwess.itemcooldowns.Lang;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ActionBar / BossBar / Scoreboard / sound / particle feedback (v2).
 * All lookups are defensive: unknown sound/particle names are ignored so
 * the plugin keeps working on every 1.21–26.3 core.
 */
public final class Visuals {

    private final ItemCooldowns plugin;
    private final Map<UUID, BossBar> bossBars = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Scoreboard> originalBoards = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Scoreboard> scoreboards = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, java.util.Set<org.bukkit.Material>> previous = new java.util.concurrent.ConcurrentHashMap<>();

    public Visuals(ItemCooldowns plugin) {
        this.plugin = plugin;
    }

    /** Periodic refresh: actionbar, bossbar, scoreboard, ready notifications and GUI refresh. */
    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.runForPlayer(player, () -> tickPlayer(player));
        }
    }

    private void tickPlayer(Player player) {
        if (!player.isOnline()) return;
        Config config = plugin.getConfigManager();
        CooldownTracker.ActiveCooldown nearest = plugin.getTracker().nearest(player);
        java.util.List<CooldownTracker.ActiveCooldown> active = plugin.getTracker().active(player);
        notifyFinished(player, active);
        updateActionBar(player, nearest);
        updateBossBar(player, nearest);
        updateScoreboard(player);
        com.sooqwess.itemcooldowns.gui.CooldownGui.refresh(plugin, player);
        if (nearest == null && !config.isBossbarShowReady()) removeBossBar(player);
        if (nearest == null && config.isScoreboardEnabled()) removeScoreboard(player);
    }

    /** Fires the "cooldown finished" chat message, sound and particle once per cooldown. */
    private void notifyFinished(Player player, java.util.List<CooldownTracker.ActiveCooldown> active) {
        UUID id = player.getUniqueId();
        java.util.Set<org.bukkit.Material> now = new java.util.HashSet<>();
        for (CooldownTracker.ActiveCooldown cd : active) {
            now.add(cd.getMaterial());
        }
        java.util.Set<org.bukkit.Material> before = previous.put(id, now);
        if (before == null) {
            return;
        }
        for (org.bukkit.Material material : before) {
            if (now.contains(material)) {
                continue;
            }
            Config.ItemRule rule = plugin.getConfigManager().ruleFor(material);
            if (rule == null) {
                continue;
            }
            plugin.getLang().sendReady(player, rule);
            playReady(player);
        }
    }

    private void updateActionBar(Player player, CooldownTracker.ActiveCooldown nearest) {
        Config config = plugin.getConfigManager();
        if (!config.isActionbarEnabled()) {
            return;
        }
        Lang lang = plugin.getLang();
        if (nearest == null) {
            if (config.isActionbarShowReady()) {
                sendActionBar(player, lang.get(player, "visual.actionbar-ready"));
            }
            return;
        }
        Config.ItemRule rule = plugin.getConfigManager().ruleFor(nearest.getMaterial());
        if (rule != null && !rule.isActionbar()) {
            return;
        }
        String item = rule != null ? lang.itemName(player, rule.getKey()) : nearest.getMaterial().name();
        String text = lang.get(player, "visual.actionbar")
                .replace("{item}", item)
                .replace("{seconds}", String.valueOf(nearest.getRemainingSeconds()))
                .replace("{progress}", progressBar(nearest.getProgress()));
        sendActionBar(player, text);
    }

    private void sendActionBar(Player player, String text) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(text));
    }

    private void updateBossBar(Player player, CooldownTracker.ActiveCooldown nearest) {
        Config config = plugin.getConfigManager();
        if (!config.isBossbarEnabled()) {
            return;
        }
        Lang lang = plugin.getLang();
        if (nearest == null) {
            if (config.isBossbarShowReady()) {
                BossBar bar = bossBar(player);
                bar.setTitle(lang.get(player, "visual.bossbar-ready"));
                bar.setProgress(1.0);
                bar.setColor(BarColor.GREEN);
            }
            return;
        }
        Config.ItemRule rule = plugin.getConfigManager().ruleFor(nearest.getMaterial());
        if (rule != null && !rule.isBossbar()) {
            removeBossBar(player);
            return;
        }
        String item = rule != null ? lang.itemName(player, rule.getKey()) : nearest.getMaterial().name();
        BossBar bar = bossBar(player);
        bar.setTitle(lang.get(player, "visual.bossbar")
                .replace("{item}", item)
                .replace("{seconds}", String.valueOf(nearest.getRemainingSeconds())));
        bar.setProgress(Math.max(0.0, Math.min(1.0, nearest.getProgress())));
        bar.setColor(BarColor.PURPLE);
        bar.setStyle(BarStyle.SOLID);
    }

    private BossBar bossBar(Player player) {
        BossBar bar = bossBars.get(player.getUniqueId());
        if (bar == null) {
            bar = Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SOLID);
            bar.addPlayer(player);
            bossBars.put(player.getUniqueId(), bar);
        } else if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
        return bar;
    }

    public void removeBossBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removePlayer(player);
        }
    }

    public void removeAllBossBars() {
        for (BossBar bar : bossBars.values()) {
            bar.removeAll();
        }
        bossBars.clear();
    }

    /** Drops all cached per-player visual state (called when a player quits or on reload). */
    public void forget(Player player) {
        removeBossBar(player);
        removeScoreboard(player);
        previous.remove(player.getUniqueId());
    }

    private void updateScoreboard(Player player) {
        Config config = plugin.getConfigManager();
        if (!config.isScoreboardEnabled()) {
            return;
        }
        Lang lang = plugin.getLang();
        java.util.List<CooldownTracker.ActiveCooldown> active = plugin.getTracker().active(player);
        if (active.isEmpty()) {
            return;
        }
        Scoreboard board = scoreboards.computeIfAbsent(player.getUniqueId(), k -> Bukkit.getScoreboardManager().getNewScoreboard());
        Objective objective = board.getObjective("ic");
        if (objective == null) {
            objective = board.registerNewObjective("ic", "dummy", lang.get(player, "visual.scoreboard-title"));
        }
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        // Clear previous entries by resetting the board.
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }
        int line = active.size() + 1;
        objective.getScore(lang.get(player, "visual.scoreboard-title")).setScore(line--);
        for (CooldownTracker.ActiveCooldown cd : active) {
            Config.ItemRule rule = plugin.getConfigManager().ruleFor(cd.getMaterial());
            String item = rule != null ? lang.itemName(player, rule.getKey()) : cd.getMaterial().name();
            String text = item + ChatColor.GRAY + ": " + ChatColor.AQUA + cd.getRemainingSeconds() + "s";
            if (text.length() > 40) {
                text = text.substring(0, 40);
            }
            objective.getScore(text).setScore(line--);
        }
        originalBoards.putIfAbsent(player.getUniqueId(), player.getScoreboard());
        player.setScoreboard(board);
    }

    private void removeScoreboard(Player player) {
        scoreboards.remove(player.getUniqueId());
        Scoreboard original = originalBoards.remove(player.getUniqueId());
        if (player.getScoreboard() != null && player.getScoreboard().getObjective("ic") != null) {
            player.setScoreboard(original != null ? original : Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void playBlocked(Player player) {
        if (plugin.getConfigManager().isSoundsEnabled()) {
            playSound(player, plugin.getConfigManager().getBlockedSound());
        }
        if (plugin.getConfigManager().isParticlesEnabled()) {
            spawnParticle(player, plugin.getConfigManager().getBlockedParticle());
        }
    }

    public void playStarted(Player player) {
        if (plugin.getConfigManager().isSoundsEnabled()) {
            playSound(player, plugin.getConfigManager().getStartedSound());
        }
    }

    public void playReady(Player player) {
        if (plugin.getConfigManager().isSoundsEnabled()) {
            playSound(player, plugin.getConfigManager().getReadySound());
        }
        if (plugin.getConfigManager().isParticlesEnabled()) {
            spawnParticle(player, plugin.getConfigManager().getReadyParticle());
        }
    }

    private void playSound(Player player, String name) {
        if (name == null || name.isEmpty() || "none".equalsIgnoreCase(name)) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(name.toUpperCase(java.util.Locale.ROOT));
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {
            // Unknown sound on this version — ignore.
        }
    }

    private void spawnParticle(Player player, String name) {
        if (name == null || name.isEmpty() || "none".equalsIgnoreCase(name)) {
            return;
        }
        try {
            Particle particle = Particle.valueOf(name.toUpperCase(java.util.Locale.ROOT));
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1.5, 0),
                    plugin.getConfigManager().getParticleCount(), 0.4, 0.4, 0.4, 0.05);
        } catch (IllegalArgumentException ignored) {
            // Unknown particle on this version — ignore.
        }
    }

    public static String progressBar(double progress) {
        int bars = 10;
        int filled = (int) Math.round(progress * bars);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? "█" : "░");
        }
        return sb.toString();
    }
}
