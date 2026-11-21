package com.sooqwess.itemcooldowns.gui;

import com.sooqwess.itemcooldowns.Config;
import com.sooqwess.itemcooldowns.CooldownTracker;
import com.sooqwess.itemcooldowns.ItemCooldowns;
import com.sooqwess.itemcooldowns.Lang;
import com.sooqwess.itemcooldowns.visual.Visuals;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Holder for the /cooldowns GUI so clicks can be cancelled safely. */
public final class CooldownGui implements InventoryHolder {

    private final Player viewer;
    private Inventory inventory;

    public CooldownGui(Player viewer) {
        this.viewer = viewer;
    }

    public static void open(ItemCooldowns plugin, Player player) {
        if (!plugin.getConfigManager().isGuiEnabled()) {
            player.sendMessage(plugin.getLang().getPrefix(player)
                    + plugin.getLang().get(player, "gui.disabled"));
            return;
        }
        CooldownGui holder = new CooldownGui(player);
        String title = plugin.getLang().get(player, "gui.title");
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.inventory = inv;
        fill(plugin, player, inv);
        player.openInventory(inv);
    }

    public static void refresh(ItemCooldowns plugin, Player player) {
        if (player.getOpenInventory() == null) {
            return;
        }
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof CooldownGui)) {
            return;
        }
        fill(plugin, player, top);
    }

    private static void fill(ItemCooldowns plugin, Player player, Inventory inv) {
        Lang lang = plugin.getLang();
        inv.clear();
        for (int i = 0; i < inv.getSize(); i++) {
            boolean border = i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8;
            if (!border) continue;
            boolean corner = i == 0 || i == 8 || i == 36 || i == 44;
            Material pane = corner ? Material.YELLOW_STAINED_GLASS_PANE
                    : (i % 2 == 0 ? Material.PURPLE_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE);
            ItemStack frame = new ItemStack(pane);
            ItemMeta fm = frame.getItemMeta();
            if (fm != null) { fm.setDisplayName(" "); frame.setItemMeta(fm); }
            inv.setItem(i, frame);
        }
        int slot = 10;
        int count = 0;
        for (Config.ItemRule rule : plugin.getConfigManager().getRules()) {
            if (!rule.isActive()) {
                continue;
            }
            if (slot >= 35) {
                break;
            }
            Material icon = rule.getMaterials().iterator().next();
            long remaining = rule.getMaterials().stream()
                    .mapToLong(m -> plugin.getTracker().remaining(player, m)).max().orElse(0L);
            boolean ready = remaining <= 0L;
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(lang.itemName(player, rule.getKey()));
                List<String> lore = new ArrayList<>();
                if (ready) {
                    lore.add(lang.get(player, "gui.ready"));
                } else {
                    long seconds = (long) Math.ceil(remaining / 1000.0);
                    lore.add(lang.get(player, "gui.remaining").replace("{seconds}", String.valueOf(seconds)));
                    double progress = 1.0 - (double) remaining / (double) (rule.getSeconds() * 1000L);
                    lore.add(lang.get(player, "gui.progress").replace("{progress}", Visuals.progressBar(progress)));
                }
                lore.add(lang.get(player, "gui.total").replace("{seconds}", String.valueOf(rule.getSeconds())));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
            count++;
            if (slot % 9 == 8) slot += 2;
        }
        if (count == 0) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(lang.get(player, "gui.empty"));
                empty.setItemMeta(meta);
            }
            inv.setItem(22, empty);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
