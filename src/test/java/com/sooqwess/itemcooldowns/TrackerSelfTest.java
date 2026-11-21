package com.sooqwess.itemcooldowns;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Dependency-free assertions; run with tools/test-tracker.sh after mvn package. */
public final class TrackerSelfTest {
    private static Player player(UUID id) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (p, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "setCooldown" -> null;
                    case "toString" -> id.toString();
                    case "hashCode" -> id.hashCode();
                    case "equals" -> p == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
    private static void check(boolean result, String name) {
        if (!result) throw new AssertionError(name);
        System.out.println("PASS " + name);
    }
    public static void main(String[] args) throws Exception {
        CooldownTracker tracker = new CooldownTracker();
        Player a = player(UUID.randomUUID()), b = player(UUID.randomUUID());
        check(tracker.startIfReady(a, Material.TRIDENT, 5, false) == 0, "first use starts");
        check(tracker.startIfReady(a, Material.TRIDENT, 5, false) > 0, "repeated use blocks");
        check(tracker.startIfReady(b, Material.TRIDENT, 5, false) == 0, "players are isolated");
        check(tracker.startIfReady(a, Material.MACE, 45, false) == 0, "materials are isolated");
        check(tracker.activeCount(a) == 2, "active count");
        tracker.clear(a, Material.TRIDENT);
        check(tracker.remaining(a, Material.TRIDENT) == 0, "per-material reset");
        check(tracker.remaining(b, Material.TRIDENT) > 0, "reset does not affect other players");
        tracker.clear(a);
        check(tracker.activeCount(a) == 0, "player reset");
        ExecutorService pool = Executors.newFixedThreadPool(8);
        AtomicInteger starts = new AtomicInteger();
        CountDownLatch gate = new CountDownLatch(1);
        for (int i = 0; i < 64; i++) pool.submit(() -> {
            try { gate.await(); } catch (InterruptedException e) { throw new RuntimeException(e); }
            if (tracker.startIfReady(a, Material.END_CRYSTAL, 45, false) == 0) starts.incrementAndGet();
        });
        gate.countDown(); pool.shutdown(); pool.awaitTermination(10, TimeUnit.SECONDS);
        check(starts.get() == 1, "64 concurrent attempts produce exactly one start");
        tracker.startIfReady(a, Material.BOW, 1, false);
        Thread.sleep(1100);
        check(tracker.remaining(a, Material.BOW) == 0, "expiry");
        tracker.clear();
        check(tracker.activeCount(a) == 0 && tracker.activeCount(b) == 0, "global reset");
        System.out.println("All 11 tracker checks passed.");
    }
}
