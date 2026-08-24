package com.sooqwess.itemcooldowns;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.lang.reflect.Method;

public final class CooldownListener implements Listener {

    private final ItemCooldowns plugin;
    private final Method useItemInHand;
    private final Method setUseItemInHand;

    public CooldownListener(ItemCooldowns plugin) {
        this.plugin = plugin;
        Method use = null;
        Method setUse = null;
        try {
            use = PlayerInteractEvent.class.getMethod("useItemInHand");
        } catch (NoSuchMethodException ignored) {
        }
        try {
            setUse = PlayerInteractEvent.class.getMethod("setUseItemInHand", Event.Result.class);
        } catch (NoSuchMethodException ignored) {
        }
        this.useItemInHand = use;
        this.setUseItemInHand = setUse;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
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
        Material mainHand = player.getInventory().getItemInMainHand().getType();
        Config.ItemRule rule = config.ruleFor(mainHand);
        if (rule != null && rule.getKind().isAttackGated()) {
            handle(player, mainHand, rule, event);
            return;
        }
        Material offHand = player.getInventory().getItemInOffHand().getType();
        rule = config.ruleFor(offHand);
        if (rule == null || !rule.getKind().isAttackGated()) {
            return;
        }
        handle(player, offHand, rule, event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getItem() == null) {
            return;
        }
        Material material = event.getItem().getType();
        Config.ItemRule rule = plugin.getConfigManager().ruleFor(material);
        if (rule == null || !rule.getKind().isUseGated()) {
            return;
        }
        if (action == Action.RIGHT_CLICK_AIR && rule.getKind() == Kind.BLOCK_USE) {
            return;
        }
        Player player = event.getPlayer();
        if (!plugin.getConfigManager().isWorldAllowed(player.getWorld())) {
            return;
        }
        if (isItemUseConsumed(event)) {
            return;
        }
        handle(player, material, rule, event);
    }

    private boolean isItemUseConsumed(PlayerInteractEvent event) {
        if (useItemInHand == null) {
            return false;
        }
        try {
            return useItemInHand.invoke(event) == Event.Result.DENY;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void handle(Player player, Material material, Config.ItemRule rule, Cancellable event) {
        if (!rule.isActive()) {
            return;
        }
        if (player.hasPermission(rule.getBypassPermission())) {
            return;
        }
        long remaining = plugin.getTracker().remaining(player, material);
        if (remaining > 0L) {
            if (rule.isCancel()) {
                cancel(player, material, event);
            }
            plugin.getLang().sendBlocked(player, rule, remaining);
            return;
        }
        plugin.getTracker().start(player, material, rule.getSeconds(), rule.isOverlay());
        plugin.getLang().sendStarted(player, rule, rule.getSeconds());
    }

    private void cancel(Player player, Material material, Cancellable event) {
        if (event instanceof PlayerInteractEvent interact && setUseItemInHand != null && isWeaponUse(material)) {
            try {
                setUseItemInHand.invoke(interact, Event.Result.DENY);
                return;
            } catch (Exception ignored) {
            }
        }
        event.setCancelled(true);
    }

    private boolean isWeaponUse(Material material) {
        Config.ItemRule rule = plugin.getConfigManager().ruleFor(material);
        return rule != null && rule.getKind() == Kind.ATTACK_AND_USE;
    }
}
