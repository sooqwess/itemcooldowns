package com.sooqwess.itemcooldowns;

import com.sooqwess.itemcooldowns.gui.CooldownGuiListener;
import com.sooqwess.itemcooldowns.hook.PapiHook;
import com.sooqwess.itemcooldowns.visual.Visuals;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemCooldowns extends JavaPlugin {

    private PvPManagerHook pvpManagerHook;
    public PvPManagerHook getPvPManagerHook() { return pvpManagerHook; }

    private Config config;
    private Lang lang;
    private CooldownTracker tracker;
    private Visuals visuals;
    private Object tickTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureConfigDefaults();
        this.config = new Config(this);
        this.lang = new Lang(this);
        this.tracker = new CooldownTracker();
        this.pvpManagerHook = new PvPManagerHook();
        getLogger().info("PvPManager hook active: " + pvpManagerHook.isActive());
        this.visuals = new Visuals(this);

        CooldownsCommand command = new CooldownsCommand(this);
        getCommand("itemcooldowns").setExecutor(command);
        getCommand("itemcooldowns").setTabCompleter(command);
        getServer().getPluginManager().registerEvents(new CooldownListener(this), this);
        getServer().getPluginManager().registerEvents(new CooldownGuiListener(), this);
        PapiHook.register(this);

        for (Config.ItemRule rule : config.getRules()) {
            if (rule.isActive()) {
                getLogger().info("Rule '" + rule.getKey() + "': " + rule.getSeconds() + "s cooldown, "
                        + rule.getMaterials().size() + " material(s), kind=" + rule.getKind());
            } else {
                getLogger().info("Rule '" + rule.getKey() + "': disabled or not supported on this server version");
            }
        }
        startVisualTicker();
        getLogger().info("ItemCooldowns v" + getDescription().getVersion() + " enabled (locale " + config.getLocale()
                + ", visuals: actionbar=" + config.isActionbarEnabled() + " bossbar=" + config.isBossbarEnabled()
                + " gui=" + config.isGuiEnabled() + "). Author: Sooqwess");
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            getLogger().info("LuckPerms found: plugin permissions are managed by LuckPerms.");
        }
    }

    @Override
    public void onDisable() {
        stopVisualTicker();
        if (visuals != null) {
            visuals.removeAllBossBars();
        }
        if (tracker != null) {
            tracker.clear();
        }
        getLogger().info("ItemCooldowns disabled.");
    }

    /**
     * Starts the periodic actionbar/bossbar/scoreboard refresher.
     * Uses the Folia global region scheduler when Folia is present, otherwise the
     * regular Bukkit scheduler. Both are only touched after a class check, so the
     * plugin still runs on Spigot/Bukkit/Purpur without exceptions.
     */
    private void startVisualTicker() {
        if (!config.isActionbarEnabled() && !config.isBossbarEnabled() && !config.isScoreboardEnabled()) {
            return;
        }
        long period = 5L; // ticks
        try {
            if (isFolia()) {
                Object scheduler = org.bukkit.Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                tickTask = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler")
                        .getMethod("runAtFixedRate", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, long.class, long.class)
                        .invoke(scheduler, this, (java.util.function.Consumer<Object>) task -> visuals.tick(), period, period);
                getLogger().info("Visual feedback scheduled via Folia global region scheduler.");
                return;
            }
        } catch (Throwable ignored) {
            getLogger().warning("Folia visual scheduler unavailable; visuals disabled rather than using an unsafe scheduler.");
            return;
        }
        try {
            tickTask = getServer().getScheduler().runTaskTimer(this, () -> visuals.tick(), period, period);
        } catch (Throwable e) {
            getLogger().warning("Could not start the visual ticker: " + e.getMessage());
        }
    }

    private void stopVisualTicker() {
        if (tickTask == null) {
            return;
        }
        try {
            if (tickTask instanceof org.bukkit.scheduler.BukkitTask task) {
                task.cancel();
            } else {
                tickTask.getClass().getMethod("cancel").invoke(tickTask);
            }
        } catch (Throwable ignored) {
        }
        tickTask = null;
    }

    /** Execute visual work on the player's owning region, never the Folia global thread. */
    public void runForPlayer(org.bukkit.entity.Player player, Runnable work) {
        if (!isFolia()) {
            work.run();
            return;
        }
        try {
            Object scheduler = org.bukkit.entity.Entity.class.getMethod("getScheduler").invoke(player);
            Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler")
                    .getMethod("run", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, Runnable.class)
                    .invoke(scheduler, this, (java.util.function.Consumer<Object>) task -> work.run(), (Runnable) () -> {});
        } catch (ReflectiveOperationException e) {
            getLogger().warning("Cannot schedule player visual update: " + e.getClass().getSimpleName());
        }
    }

    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void reload() {
        reloadConfig();
        ensureConfigDefaults();
        this.config = new Config(this);
        this.lang.clearCache();
        this.tracker.clear();
        this.visuals.removeAllBossBars();
        stopVisualTicker();
        startVisualTicker();
    }

    /** Adds new configuration keys when an existing installation is updated. */
    private void ensureConfigDefaults() {
        getConfig().addDefault("creative-to-survival-on-player-hit", false);
        getConfig().addDefault("locale", "auto");
        getConfig().addDefault("messages.notify-ready", true);
        getConfig().addDefault("visuals.actionbar.enabled", true);
        getConfig().addDefault("visuals.bossbar.enabled", true);
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    public Config getConfigManager() {
        return config;
    }

    public Lang getLang() {
        return lang;
    }

    public CooldownTracker getTracker() {
        return tracker;
    }

    public Visuals getVisuals() {
        return visuals;
    }
}
