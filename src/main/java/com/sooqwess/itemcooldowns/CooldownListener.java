package com.sooqwess.itemcooldowns;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.lang.reflect.Method;

public final class CooldownListener implements Listener {

    private final ItemCooldowns plugin;
    private final java.util.Map<java.util.UUID, Long> harmlessLungeUntil = new java.util.concurrent.ConcurrentHashMap<>();
    private org.bukkit.NamespacedKey harmlessProjectileKey() {
        return new org.bukkit.NamespacedKey(plugin, "harmless-use");
    }
    private final java.util.Map<java.util.UUID, Long> lastBlockedFeedback = new java.util.concurrent.ConcurrentHashMap<>();
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Trident trident && trident.getShooter() instanceof Player shooter) {
            Config config = plugin.getConfigManager();
            if (!config.isWorldAllowed(shooter.getWorld())) return;
            if (event.getEntity() instanceof Player victim && config.isPvPManagerEnabled()
                    && !plugin.getPvPManagerHook().canAttack(shooter, victim)) return;
            if (trident.getPersistentDataContainer().has(harmlessProjectileKey(), org.bukkit.persistence.PersistentDataType.BYTE)) {
                event.setCancelled(true);
            }
            return;
        }
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
                && !plugin.getPvPManagerHook().canAttack(player, victim)) return;
        Long harmlessUntil = harmlessLungeUntil.get(player.getUniqueId());
        if (harmlessUntil != null && System.currentTimeMillis() < harmlessUntil) {
            event.setCancelled(true);
            return;
        }
        if (config.isCreativeToSurvivalOnPlayerHit() && event.getEntity() instanceof Player
                && player.getGameMode() == GameMode.CREATIVE) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        // Vanilla melee attacks always use the main hand. Checking the off hand here
        // would incorrectly block a fist/sword hit just because a cooldown item is held there.
        Material mainHand = player.getInventory().getItemInMainHand().getType();
        Config.ItemRule rule = config.ruleFor(mainHand);
        if (rule != null && rule.getKind().isAttackGated()) {
            handle(player, mainHand, rule, event);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Do not retain a player's cooldown or make it survive reconnecting.
        lastBlockedFeedback.remove(event.getPlayer().getUniqueId());
        harmlessLungeUntil.remove(event.getPlayer().getUniqueId());
        plugin.getTracker().clear(event.getPlayer());
        plugin.getVisuals().forget(event.getPlayer());
    }

    /** Handles Riptide separately because it does not create a trident projectile. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();
        Config config = plugin.getConfigManager();
        if (!config.isWorldAllowed(player.getWorld())) {
            return;
        }
        Config.ItemRule rule = config.ruleFor(Material.TRIDENT);
        if (rule != null) {
            if (rule.getUseMode() == Config.ItemRule.UseMode.BLOCK) {
                handle(player, Material.TRIDENT, rule, event);
            } else if (!player.hasPermission(rule.getBypassPermission())) {
                long remaining = plugin.getTracker().startIfReady(player, Material.TRIDENT, rule.getSeconds(), false);
                if (remaining > 0L || rule.getUseMode() == Config.ItemRule.UseMode.NO_DAMAGE)
                    harmlessLungeUntil.put(player.getUniqueId(), System.currentTimeMillis() + 1500L);
            }
        }
    }

    /** Starts the trident cooldown when a throw actually succeeds, not while it is being charged. */
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
        if (rule != null) {
            if (rule.getUseMode() == Config.ItemRule.UseMode.BLOCK) {
                handle(player, Material.TRIDENT, rule, event);
            } else if (!player.hasPermission(rule.getBypassPermission())) {
                // Launch still starts the timer, but movement/projectiles are allowed.
                // Tag this projectile rather than checking its shooter's current cooldown on impact:
                // a valid first throw must not cancel its own damage.
                long remaining = plugin.getTracker().startIfReady(player, Material.TRIDENT, rule.getSeconds(), false);
                if (remaining > 0L || rule.getUseMode() == Config.ItemRule.UseMode.NO_DAMAGE)
                    trident.getPersistentDataContainer().set(harmlessProjectileKey(), org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                if (remaining == 0L) {
                    plugin.getLang().sendStarted(player, rule, rule.getSeconds());
                    plugin.getVisuals().playStarted(player);
                } else feedbackBlocked(player, rule, remaining);
            }
        }
    }

    /** Bow shots: cooldown starts on the actual shot, not on draw. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Config config = plugin.getConfigManager();
        if (!config.isWorldAllowed(player.getWorld())) {
            return;
        }
        Material bow = event.getBow() != null ? event.getBow().getType() : Material.BOW;
        Config.ItemRule rule = config.ruleFor(bow);
        if (rule != null && rule.getKind() == Kind.SHOOT_ONLY) {
            handle(player, bow, rule, event);
        }
    }

    /** Totem consumption starts its cooldown after the resurrect succeeds. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTotemResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Config config = plugin.getConfigManager();
        if (!config.isWorldAllowed(player.getWorld())) {
            return;
        }
        Config.ItemRule rule = config.ruleFor(Material.TOTEM_OF_UNDYING);
        if (rule != null && rule.getKind() == Kind.RESURRECT_ONLY) {
            handle(player, Material.TOTEM_OF_UNDYING, rule, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player actor = event.getPlayer();
        if (!plugin.getConfigManager().isWorldAllowed(actor.getWorld())) return;
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.RESPAWN_ANCHOR
                && event.useInteractedBlock() != Event.Result.DENY) {
            org.bukkit.inventory.ItemStack held = event.getItem();
            if (held == null || held.getType() == Material.AIR || !held.getType().isBlock()) {
                Config.ItemRule anchor = plugin.getConfigManager().ruleFor(Material.RESPAWN_ANCHOR);
                if (anchor != null) { handle(actor, Material.RESPAWN_ANCHOR, anchor, event); return; }
            }
        }
        if (event.getItem() == null) {
            return;
        }
        Material material = event.getItem().getType();
        Config.ItemRule rule = plugin.getConfigManager().ruleFor(material);
        if (rule == null || !rule.getKind().isUseGated()) {
            return;
        }
        // A trident must only start cooldown after ProjectileLaunchEvent. Starting it here
        // cancels the throw itself and lets same-tick attacks bypass the tracker.
        if (material == Material.TRIDENT) {
            return;
        }
        if (rule.getKind() == Kind.ATTACK_AND_USE && rule.getUseMode() != Config.ItemRule.UseMode.BLOCK) {
            if (rule.getUseMode() == Config.ItemRule.UseMode.NO_DAMAGE && !actor.hasPermission(rule.getBypassPermission()))
                harmlessLungeUntil.put(actor.getUniqueId(), System.currentTimeMillis() + 1500L);
            return;
        }
        // Bows are handled by PlayerShootBowEvent (actual shot), not by draw.
        if (material == Material.BOW) {
            return;
        }
        if (action == Action.RIGHT_CLICK_AIR && rule.getKind() == Kind.BLOCK_USE
                && (material == Material.END_CRYSTAL || material == Material.RESPAWN_ANCHOR)) {
            // Crystals/anchors are placed on blocks; air clicks do nothing.
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
        if (!rule.isActive() || player.hasPermission(rule.getBypassPermission())) {
            return;
        }
        long remaining = plugin.getTracker().startIfReady(player, material, rule.getSeconds(), rule.isOverlay());
        if (remaining > 0L) {
            if (rule.isCancel()) {
                cancel(player, material, event);
            }
            feedbackBlocked(player, rule, remaining);
            return;
        }
        plugin.getLang().sendStarted(player, rule, rule.getSeconds());
        plugin.getVisuals().playStarted(player);
    }

    private void feedbackBlocked(Player player, Config.ItemRule rule, long remaining) {
        long now = System.currentTimeMillis();
        Long last = lastBlockedFeedback.get(player.getUniqueId());
        if (last == null || now - last >= 1000L) {
            lastBlockedFeedback.put(player.getUniqueId(), now);
            plugin.getLang().sendBlocked(player, rule, remaining);
            plugin.getVisuals().playBlocked(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrystalInteract(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof org.bukkit.entity.EnderCrystal)) return;
        Player player = event.getPlayer();
        if (!plugin.getConfigManager().isWorldAllowed(player.getWorld())) return;
        Material held = (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand()).getType();
        if (held != Material.END_CRYSTAL) return;
        Config.ItemRule rule = plugin.getConfigManager().ruleFor(held);
        if (rule != null) handle(player, held, rule, event);
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
