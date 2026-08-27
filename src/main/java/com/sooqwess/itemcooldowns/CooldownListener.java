package com.sooqwess.itemcooldowns;

import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownListener implements Listener {

    private static final long MESSAGE_THROTTLE_MS = 3000;

    private final ItemCooldowns plugin;
    private final Map<UUID, Long> lastBlockedMessage = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();

    public CooldownListener(ItemCooldowns plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        Config config = plugin.getConfigManager();
        if (!config.isWorldAllowed(player.getWorld())) {
            return;
        }
        if (config.isPvpOnly() && !(event.getEntity() instanceof Player)) {
            return;
        }
        if (event.getEntity() instanceof Player victim && config.isPvPManagerEnabled()
                && plugin.getPvPManagerHook().isActive()
                && !plugin.getPvPManagerHook().canAttack(player, victim)) {
            return;
        }
        if (event.getEntity() instanceof Player && config.isCreativeToSurvivalOnPlayerHit()
                && player.getGameMode() == org.bukkit.GameMode.CREATIVE
                && !player.hasPermission("itemcooldowns.bypass")) {
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        }
        if (plugin.getTracker().remaining(player, Material.TRIDENT) > 0L
                && isLungeDamage(player)) {
            event.setCancelled(true);
            return;
        }
        Material mainHand = player.getInventory().getItemInMainHand().getType();
        Config.ItemRule rule = config.ruleFor(mainHand);
        if (rule != null && rule.getKind().isAttackGated()) {
            handle(player, mainHand, rule, event);
        }
    }

    private boolean isLungeDamage(Player player) {
        Long last = lastUse.get(player.getUniqueId());
        return last != null && System.currentTimeMillis() - last <= 1500L;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTridentLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) {
            return;
        }
        ProjectileSource source = trident.getShooter();
        if (!(source instanceof Player player)) {
            return;
        }
        Config config = plugin.getConfigManager();
        if (!config.isWorldAllowed(player.getWorld())) {
            return;
        }
        Config.ItemRule rule = config.ruleFor(Material.TRIDENT);
        if (rule == null || !rule.getKind().isUseGated() || rule.getUseMode() != Config.ItemRule.UseMode.BLOCK) {
            return;
        }
        handle(player, Material.TRIDENT, rule, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        Config config = plugin.getConfigManager();
        if (!config.isWorldAllowed(player.getWorld())) {
            return;
        }
        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && event.getClickedBlock().getType() == Material.RESPAWN_ANCHOR
                && event.useInteractedBlock() != Event.Result.DENY) {
            ItemStack handItem = handItem(event, player);
            if (handItem.getType() == Material.AIR || !handItem.getType().isBlock()) {
                Config.ItemRule anchorRule = config.ruleFor(Material.RESPAWN_ANCHOR);
                if (anchorRule != null && anchorRule.getKind() == Kind.BLOCK_USE) {
                    handle(player, Material.RESPAWN_ANCHOR, anchorRule, event);
                    return;
                }
            }
        }
        if (event.getItem() == null) {
            return;
        }
        Material material = event.getItem().getType();
        Config.ItemRule rule = config.ruleFor(material);
        if (rule == null || !rule.getKind().isUseGated()) {
            return;
        }
        if (material == Material.TRIDENT) {
            return;
        }
        if (rule.getUseMode() == Config.ItemRule.UseMode.DAMAGE) {
            return;
        }
        if (rule.getUseMode() == Config.ItemRule.UseMode.NO_DAMAGE) {
            lastUse.put(player.getUniqueId(), System.currentTimeMillis());
            return;
        }
        if (action == Action.RIGHT_CLICK_AIR && rule.getKind() == Kind.BLOCK_USE) {
            return;
        }
        if (action == Action.RIGHT_CLICK_BLOCK && event.useInteractedBlock() == Event.Result.ALLOW) {
            return;
        }
        if (event.useItemInHand() == Event.Result.DENY && plugin.getTracker().remaining(player, material) <= 0L) {
            return;
        }
        handle(player, material, rule, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof EnderCrystal)) {
            return;
        }
        Player player = event.getPlayer();
        Config config = plugin.getConfigManager();
        if (!config.isWorldAllowed(player.getWorld())) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            item = player.getInventory().getItemInOffHand();
        }
        if (item.getType() != Material.END_CRYSTAL) {
            return;
        }
        Config.ItemRule rule = config.ruleFor(Material.END_CRYSTAL);
        if (rule == null || rule.getKind() != Kind.BLOCK_USE) {
            return;
        }
        handle(player, Material.END_CRYSTAL, rule, event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastBlockedMessage.remove(uuid);
        lastUse.remove(uuid);
        plugin.getTracker().clear(event.getPlayer());
    }

    private ItemStack handItem(PlayerInteractEvent event, Player player) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return player.getInventory().getItemInOffHand();
        }
        return player.getInventory().getItemInMainHand();
    }

    private void handle(Player player, Material material, Config.ItemRule rule, Cancellable event) {
        if (!rule.isActive() || player.hasPermission(rule.getBypassPermission())) {
            return;
        }
        long remaining = plugin.getTracker().remaining(player, material);
        if (remaining > 0L) {
            if (rule.isCancel()) {
                event.setCancelled(true);
            }
            long now = System.currentTimeMillis();
            Long last = lastBlockedMessage.get(player.getUniqueId());
            if (last == null || now - last >= MESSAGE_THROTTLE_MS) {
                lastBlockedMessage.put(player.getUniqueId(), now);
                plugin.getLang().sendBlocked(player, rule, remaining);
            }
            return;
        }
        plugin.getTracker().start(player, material, rule.getSeconds(), rule.isOverlay());
        plugin.getLang().sendStarted(player, rule, rule.getSeconds());
    }
}
