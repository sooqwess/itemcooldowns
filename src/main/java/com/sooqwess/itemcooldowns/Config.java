package com.sooqwess.itemcooldowns;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Plugin configuration (v2). Reads {@code config.yml} once on enable/reload.
 * Missing keys fall back to safe defaults so old v1 configs keep working.
 */
public final class Config {

    private final boolean pvpManagerEnabled;
    private final String locale;
    private final boolean pvpOnly;
    private final boolean creativeToSurvivalOnPlayerHit;
    private final boolean worldsEnabled;
    private final boolean worldsWhitelist;
    private final List<String> worlds;
    private final boolean messagesEnabled;
    private final boolean notifyStart;
    private final boolean notifyReady;

    private final boolean actionbarEnabled;
    private final boolean actionbarShowReady;
    private final boolean bossbarEnabled;
    private final boolean bossbarShowReady;
    private final boolean scoreboardEnabled;
    private final boolean guiEnabled;
    private final boolean soundsEnabled;
    private final boolean particlesEnabled;
    private final String blockedSound;
    private final String readySound;
    private final String startedSound;
    private final String blockedParticle;
    private final String readyParticle;
    private final int particleCount;

    private final Map<Material, ItemRule> rulesByMaterial = new HashMap<>();
    private final Map<String, ItemRule> rulesByKey = new HashMap<>();
    private final List<ItemRule> rules = new ArrayList<>();

    public Config(JavaPlugin plugin) {
        FileConfiguration c = plugin.getConfig();
        this.pvpManagerEnabled = c.getBoolean("pvpmanager.enabled", true);
        this.locale = c.getString("locale", "auto").toLowerCase(Locale.ROOT);
        this.pvpOnly = c.getBoolean("pvp-only", true);
        this.creativeToSurvivalOnPlayerHit = c.getBoolean("creative-to-survival-on-player-hit", false) || c.getBoolean("gamemode.switch-creative-to-survival", false);
        this.worldsEnabled = c.getBoolean("worlds.enabled", false);
        this.worldsWhitelist = c.getBoolean("worlds.whitelist", true);
        this.worlds = c.getStringList("worlds.list");
        this.messagesEnabled = c.getBoolean("messages.enabled", true);
        this.notifyStart = c.getBoolean("messages.notify-start", false);
        this.notifyReady = c.getBoolean("messages.notify-ready", true);

        this.actionbarEnabled = c.getBoolean("visuals.actionbar.enabled", true);
        this.actionbarShowReady = c.getBoolean("visuals.actionbar.show-ready", false);
        this.bossbarEnabled = c.getBoolean("visuals.bossbar.enabled", true);
        this.bossbarShowReady = c.getBoolean("visuals.bossbar.show-ready", false);
        this.scoreboardEnabled = c.getBoolean("visuals.scoreboard.enabled", true);
        this.guiEnabled = c.getBoolean("visuals.gui.enabled", true);
        this.soundsEnabled = c.getBoolean("visuals.sounds.enabled", true);
        this.particlesEnabled = c.getBoolean("visuals.particles.enabled", true);
        this.blockedSound = c.getString("visuals.sounds.blocked", "ENTITY_VILLAGER_NO");
        this.readySound = c.getString("visuals.sounds.ready", "BLOCK_NOTE_BLOCK_PLING");
        this.startedSound = c.getString("visuals.sounds.started", "BLOCK_NOTE_BLOCK_HARP");
        this.blockedParticle = c.getString("visuals.particles.blocked", "SMOKE_NORMAL");
        this.readyParticle = c.getString("visuals.particles.ready", "HAPPY_VILLAGER");
        this.particleCount = Math.max(1, Math.min(64, c.getInt("visuals.particles.count", 8)));

        register(new ItemRule(c, "mace", Kind.ATTACK_ONLY, 45, "itemcooldowns.bypass.mace", "MACE"));
        register(new ItemRule(c, "spear", Kind.ATTACK_AND_USE, 5, "itemcooldowns.bypass.spear",
                "WOODEN_SPEAR", "STONE_SPEAR", "COPPER_SPEAR", "IRON_SPEAR", "GOLDEN_SPEAR", "DIAMOND_SPEAR", "NETHERITE_SPEAR"));
        register(new ItemRule(c, "trident", Kind.ATTACK_AND_USE, 5, "itemcooldowns.bypass.trident", "TRIDENT"));
        register(new ItemRule(c, "end-crystal", Kind.BLOCK_USE, 45, "itemcooldowns.bypass.end-crystal", "END_CRYSTAL"));
        register(new ItemRule(c, "respawn-anchor", Kind.BLOCK_USE, 45, "itemcooldowns.bypass.respawn-anchor", "RESPAWN_ANCHOR"));
        register(new ItemRule(c, "ender-pearl", Kind.BLOCK_USE, 5, "itemcooldowns.bypass.ender-pearl", "ENDER_PEARL"));
        register(new ItemRule(c, "bow", Kind.SHOOT_ONLY, 2, "itemcooldowns.bypass.bow", "BOW"));
        register(new ItemRule(c, "crossbow", Kind.SHOOT_ONLY, 5, "itemcooldowns.bypass.crossbow", "CROSSBOW"));
        register(new ItemRule(c, "shield", Kind.BLOCK_USE, 5, "itemcooldowns.bypass.shield", "SHIELD"));
        register(new ItemRule(c, "totem", Kind.RESURRECT_ONLY, 30, "itemcooldowns.bypass.totem", "TOTEM_OF_UNDYING"));
        register(new ItemRule(c, "wind-charge", Kind.BLOCK_USE, 3, "itemcooldowns.bypass.wind-charge", "WIND_CHARGE"));
    }

    private void register(ItemRule rule) {
        rules.add(rule);
        rulesByKey.put(rule.getKey(), rule);
        if (rule.isActive()) {
            for (Material material : rule.getMaterials()) {
                rulesByMaterial.put(material, rule);
            }
        }
    }

    public ItemRule ruleFor(Material material) {
        return rulesByMaterial.get(material);
    }

    public ItemRule ruleForKey(String key) {
        if (key == null) {
            return null;
        }
        return rulesByKey.get(key.toLowerCase(Locale.ROOT));
    }

    public List<ItemRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    public boolean isWorldAllowed(org.bukkit.World world) {
        if (!worldsEnabled) {
            return true;
        }
        boolean listed = worlds.contains(world.getName());
        return worldsWhitelist ? listed : !listed;
    }

    public String getLocale() {
        return locale;
    }

    public boolean isPvPManagerEnabled() { return pvpManagerEnabled; }

    public boolean isPvpOnly() {
        return pvpOnly;
    }

    public boolean isCreativeToSurvivalOnPlayerHit() {
        return creativeToSurvivalOnPlayerHit;
    }

    public boolean isMessagesEnabled() {
        return messagesEnabled;
    }

    public boolean isNotifyStart() {
        return notifyStart;
    }

    public boolean isNotifyReady() {
        return notifyReady;
    }

    public boolean isActionbarEnabled() {
        return actionbarEnabled;
    }

    public boolean isActionbarShowReady() {
        return actionbarShowReady;
    }

    public boolean isBossbarEnabled() {
        return bossbarEnabled;
    }

    public boolean isBossbarShowReady() {
        return bossbarShowReady;
    }

    public boolean isScoreboardEnabled() {
        return scoreboardEnabled;
    }

    public boolean isGuiEnabled() {
        return guiEnabled;
    }

    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    public boolean isParticlesEnabled() {
        return particlesEnabled;
    }

    public String getBlockedSound() {
        return blockedSound;
    }

    public String getReadySound() {
        return readySound;
    }

    public String getStartedSound() {
        return startedSound;
    }

    public String getBlockedParticle() {
        return blockedParticle;
    }

    public String getReadyParticle() {
        return readyParticle;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public static final class ItemRule {
        public enum UseMode {
            BLOCK, DAMAGE, NO_DAMAGE;
            public static UseMode parse(String value) {
                return switch (value == null ? "" : value.toLowerCase(Locale.ROOT)) {
                    case "damage" -> DAMAGE;
                    case "no-damage", "allow", "nodamage" -> NO_DAMAGE;
                    default -> BLOCK;
                };
            }
        }
        private final UseMode useMode;
        public UseMode getUseMode() { return useMode; }


        private final String key;
        private final Kind kind;
        private final Set<Material> materials;
        private final boolean enabled;
        private final int seconds;
        private final boolean cancel;
        private final boolean overlay;
        private final String bypassPermission;
        private final boolean actionbar;
        private final boolean bossbar;

        ItemRule(FileConfiguration c, String key, Kind kind, int defaultSeconds, String defaultPermission, String... materialNames) {
            this.key = key;
            this.kind = kind;
            this.enabled = c.getBoolean(key + ".enabled", true);
            this.seconds = Math.max(0, c.getInt(key + ".cooldown-seconds", defaultSeconds));
            this.cancel = c.getBoolean(key + ".cancel-action", true);
            this.useMode = c.contains(key + ".use-mode")
                    ? UseMode.parse(c.getString(key + ".use-mode"))
                    : (c.contains(key + ".block-use") && !c.getBoolean(key + ".block-use")
                    ? UseMode.DAMAGE : UseMode.BLOCK);
            this.overlay = c.getBoolean(key + ".overlay", true);
            this.bypassPermission = c.getString(key + ".bypass-permission", defaultPermission);
            this.actionbar = c.getBoolean(key + ".actionbar", true);
            this.bossbar = c.getBoolean(key + ".bossbar", true);
            Set<Material> resolved = new java.util.LinkedHashSet<>();
            for (String name : materialNames) {
                Material material = Material.matchMaterial(name);
                if (material != null) {
                    resolved.add(material);
                }
            }
            this.materials = Collections.unmodifiableSet(resolved);
        }

        public boolean isActive() {
            return enabled && !materials.isEmpty();
        }

        public String getKey() {
            return key;
        }

        public Kind getKind() {
            return kind;
        }

        public Set<Material> getMaterials() {
            return materials;
        }

        public int getSeconds() {
            return seconds;
        }

        public boolean isCancel() {
            return cancel;
        }

        public boolean isOverlay() {
            return overlay;
        }

        public String getBypassPermission() {
            return bypassPermission;
        }

        public boolean isActionbar() {
            return actionbar;
        }

        public boolean isBossbar() {
            return bossbar;
        }
    }
}
