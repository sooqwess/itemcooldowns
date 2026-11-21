package com.sooqwess.itemcooldowns;

import com.sooqwess.itemcooldowns.gui.CooldownGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CooldownsCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERMISSION = "itemcooldowns.admin";
    private static final String USE_PERMISSION = "itemcooldowns.use";

    private final ItemCooldowns plugin;

    public CooldownsCommand(ItemCooldowns plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Lang lang = plugin.getLang();
        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (!player.hasPermission(USE_PERMISSION)) {
                    player.sendMessage(lang.getPrefix(player) + lang.get(player, "commands.no-permission"));
                    return true;
                }
                CooldownGui.open(plugin, player);
            } else {
                sender.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "commands.help"));
            }
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> {
                sender.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "commands.help"));
                return true;
            }
            case "gui", "menu", "cooldowns" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "commands.players-only"));
                    return true;
                }
                if (!player.hasPermission(USE_PERMISSION)) {
                    player.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "commands.no-permission"));
                    return true;
                }
                CooldownGui.open(plugin, player);
                return true;
            }
            case "check", "time" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "commands.players-only"));
                    return true;
                }
                String key = args.length > 1 ? args[1] : null;
                if (key == null) {
                    CooldownTracker.ActiveCooldown nearest = plugin.getTracker().nearest(player);
                    if (nearest == null) {
                        player.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "commands.check-ready"));
                    } else {
                        player.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "commands.check-nearest")
                                .replace("{seconds}", String.valueOf(nearest.getRemainingSeconds())));
                    }
                    return true;
                }
                Config.ItemRule rule = plugin.getConfigManager().ruleForKey(key);
                if (rule == null || rule.getMaterials().isEmpty()) {
                    player.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "commands.unknown-item").replace("{item}", key));
                    return true;
                }
                Material material = rule.getMaterials().iterator().next();
                long remaining = rule.getMaterials().stream()
                        .mapToLong(m -> plugin.getTracker().remaining(player, m)).max().orElse(0L);
                if (remaining <= 0L) {
                    player.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "cooldown.ready")
                            .replace("{item}", lang.itemName(player, rule.getKey())));
                } else {
                    player.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "cooldown.blocked")
                            .replace("{item}", lang.itemName(player, rule.getKey()))
                            .replace("{seconds}", String.valueOf((long) Math.ceil(remaining / 1000.0))));
                }
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "commands.no-permission"));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(plugin.getLang().getPrefix() + plugin.getLang().get("commands.reloaded"));
                return true;
            }
            case "reset", "clear" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "commands.no-permission"));
                    return true;
                }
                String target = args.length > 1 ? args[1] : (sender instanceof Player p ? p.getName() : null);
                if (target == null) {
                    sender.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "commands.usage-reset"));
                    return true;
                }
                Player player = Bukkit.getPlayerExact(target);
                if (player == null) {
                    sender.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "commands.player-not-found").replace("{player}", target));
                    return true;
                }
                plugin.runForPlayer(player, () -> plugin.getTracker().clear(player));
                sender.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "commands.reset").replace("{player}", player.getName()));
                return true;
            }
            default -> {
                sender.sendMessage(lang.getPrefix(sender instanceof Player p ? p : null) + lang.get(sender instanceof Player p ? p : null, "commands.unknown"));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("help");
            if (sender instanceof Player) {
                completions.add("gui");
                completions.add("check");
            }
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                completions.add("reload");
                completions.add("reset");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            for (Config.ItemRule rule : plugin.getConfigManager().getRules()) {
                if (rule.isActive()) {
                    completions.add(rule.getKey());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        String current = args.length > 0 ? args[args.length - 1].toLowerCase(Locale.ROOT) : "";
        completions.removeIf(c -> !c.toLowerCase(Locale.ROOT).startsWith(current));
        return completions;
    }
}
