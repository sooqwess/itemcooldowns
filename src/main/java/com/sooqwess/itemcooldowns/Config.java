package com.sooqwess.itemcooldowns;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Config {

    private final FileConfiguration c;
    private final String locale;
    private final boolean pvpOnly;
    private final boolean worldsEnabled;
    private final List<String> worlds;
    private final boolean messagesEnabled;
    private final boolean notifyStart;
    private final Map<Material, ItemRule> rulesByMaterial = new HashMap<>();
    private final List<ItemRule> rules = new ArrayList<>();

    public Config(JavaPlugin plugin) {
        this.c = plugin.getConfig();
        this.locale = c.getString("locale", "en");
        this.pvpOnly = c.getBoolean("pvp-only", true);
        this.worldsEnabled = c.getBoolean("worlds.enabled", false);
        this.worlds = c.getStringList("worlds.list");
        this.messagesEnabled = c.getBoolean("messages.enabled", true);
        this.notifyStart = c.getBoolean("messages.notify-start", false);
        register(new ItemRule(c, "mace", Kind.ATTACK_ONLY, 45, "itemcooldowns.bypass.mace", "MACE"));
        register(new ItemRule(c, "spear", Kind.ATTACK_AND_USE, 5, "itemcooldowns.bypass.spear",
                "WOODEN_SPEAR", "STONE_SPEAR", "COPPER_SPEAR", "IRON_SPEAR", "GOLDEN_SPEAR", "DIAMOND_SPEAR", "NETHERITE_SPEAR"));
        register(new ItemRule(c, "trident", Kind.ATTACK_AND_USE, 5, "itemcooldowns.bypass.trident", "TRIDENT"));
        register(new ItemRule(c, "end-crystal", Kind.BLOCK_USE, 45, "itemcooldowns.bypass.end-crystal", "END_CRYSTAL"));
        register(new ItemRule(c, "respawn-anchor", Kind.BLOCK_USE, 45, "itemcooldowns.bypass.respawn-anchor", "RESPAWN_ANCHOR"));
    }

    private void register(ItemRule rule) {
        rules.add(rule);
        if (rule.isActive()) {
            for (Material material : rule.getMaterials()) {
                rulesByMaterial.put(material, rule);
            }
        }
    }

    public ItemRule ruleFor(Material material) {
        return rulesByMaterial.get(material);
    }

    public List<ItemRule> getRules() {
        return rules;
    }

    public boolean isWorldAllowed(org.bukkit.World world) {
        return !worldsEnabled || worlds.contains(world.getName());
    }

    public String getLocale() {
        return locale;
    }

    public boolean isPvpOnly() {
        return pvpOnly;
    }

    public boolean isMessagesEnabled() {
        return messagesEnabled;
    }

    public boolean isNotifyStart() {
        return notifyStart;
    }

    public boolean isCreativeToSurvivalOnPlayerHit() {
        if (c.contains("creative-to-survival-on-player-hit")) {
            return c.getBoolean("creative-to-survival-on-player-hit", false);
        }
        return c.getBoolean("gamemode.switch-creative-to-survival", false);
    }

    public boolean isPvPManagerEnabled() {
        return c.getBoolean("pvpmanager.enabled", true);
    }

    public static final class ItemRule {

        public enum UseMode {
            BLOCK,
            DAMAGE,
            NO_DAMAGE;

            public static UseMode parse(String value) {
                return switch (value == null ? "" : value.toLowerCase()) {
                    case "block", "true" -> BLOCK;
                    case "no-damage", "allow", "nodamage" -> NO_DAMAGE;
                    default -> DAMAGE;
                };
            }
        }

        private final String key;
        private final Kind kind;
        private final Set<Material> materials;
        private final boolean enabled;
        private final int seconds;
        private final boolean cancel;
        private final UseMode useMode;
        private final boolean overlay;
        private final String bypassPermission;

        ItemRule(FileConfiguration c, String key, Kind kind, int defaultSeconds, String defaultPermission, String... materialNames) {
            this.key = key;
            this.kind = kind;
            this.enabled = c.getBoolean(key + ".enabled", true);
            this.seconds = Math.max(0, c.getInt(key + ".cooldown-seconds", defaultSeconds));
            this.cancel = c.getBoolean(key + ".cancel-action", true);
            if (c.contains(key + ".use-mode")) {
                this.useMode = UseMode.parse(c.getString(key + ".use-mode"));
            } else if (c.contains(key + ".block-use")) {
                this.useMode = c.getBoolean(key + ".block-use") ? UseMode.BLOCK : UseMode.DAMAGE;
            } else {
                this.useMode = kind == Kind.ATTACK_AND_USE ? UseMode.DAMAGE : UseMode.BLOCK;
            }
            this.overlay = c.getBoolean(key + ".overlay", true);
            this.bypassPermission = c.getString(key + ".bypass-permission", defaultPermission);
            Set<Material> resolved = new HashSet<>();
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

        public UseMode getUseMode() {
            return useMode;
        }

        public boolean isOverlay() {
            return overlay;
        }

        public String getBypassPermission() {
            return bypassPermission;
        }
    }
}
