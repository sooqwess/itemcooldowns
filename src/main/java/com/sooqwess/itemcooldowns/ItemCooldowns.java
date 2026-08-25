package com.sooqwess.itemcooldowns;

import org.bukkit.plugin.java.JavaPlugin;

public final class ItemCooldowns extends JavaPlugin {

    private Config config;
    private Lang lang;
    private CooldownTracker tracker;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureConfigDefaults();
        this.config = new Config(this);
        this.lang = new Lang(this, config.getLocale());
        this.tracker = new CooldownTracker();
        CooldownsCommand command = new CooldownsCommand(this);
        getCommand("itemcooldowns").setExecutor(command);
        getCommand("itemcooldowns").setTabCompleter(command);
        getServer().getPluginManager().registerEvents(new CooldownListener(this), this);
        for (Config.ItemRule rule : config.getRules()) {
            if (rule.isActive()) {
                getLogger().info("Rule '" + rule.getKey() + "': " + rule.getSeconds() + "s cooldown, " + rule.getMaterials().size() + " material(s)");
            } else {
                getLogger().info("Rule '" + rule.getKey() + "': disabled or not supported on this server version");
            }
        }
        getLogger().info("ItemCooldowns v" + getPluginMeta().getVersion() + " enabled. Author: Sooqwess");
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            getLogger().info("LuckPerms found: plugin permissions are managed by LuckPerms.");
        } else {
            getLogger().info("LuckPerms not found: using built-in permission defaults.");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("ItemCooldowns disabled.");
    }

    public void reload() {
        reloadConfig();
        ensureConfigDefaults();
        this.config = new Config(this);
        this.lang = new Lang(this, config.getLocale());
        this.tracker.clear();
    }

    /** Adds new configuration keys when an existing installation is updated. */
    private void ensureConfigDefaults() {
        getConfig().addDefault("creative-to-survival-on-player-hit", false);
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
}
