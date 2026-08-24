package com.sooqwess.itemcooldowns;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public final class CooldownsCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERMISSION = "itemcooldowns.admin";

    private final ItemCooldowns plugin;

    public CooldownsCommand(ItemCooldowns plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(plugin.getLang().getPrefix() + plugin.getLang().get("commands.help"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(ADMIN_PERMISSION)) {
                sender.sendMessage(plugin.getLang().getPrefix() + plugin.getLang().get("commands.no-permission"));
                return true;
            }
            plugin.reload();
            sender.sendMessage(plugin.getLang().getPrefix() + plugin.getLang().get("commands.reloaded"));
            return true;
        }
        sender.sendMessage(plugin.getLang().getPrefix() + plugin.getLang().get("commands.unknown"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("help");
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                completions.add("reload");
            }
        }
        return completions;
    }
}
