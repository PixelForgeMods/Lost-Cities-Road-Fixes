package net.austizz.lostcitiesroadfixes.planning;

import net.austizz.lostcitiesroadfixes.road.PlanningRegion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Timeout(10)
class RegionalPlanningDeterminismTest {
    private static final long WORLD_SEED = -6_377_442_428_365_110_436L;

    @Test
    void derivesTheSameSeedsRegardlessOfRequestOrder() {
        List<RoadPlanKey> keys = List.of(
                key(new PlanningRegion(-1, -3)),
                key(new PlanningRegion(-1, -2)),
                key(new PlanningRegion(0, -2)),
                key(new PlanningRegion(1, 4)));

        Map<RoadPlanKey, Long> forward = deriveInOrder(keys);
        List<RoadPlanKey> reversedKeys = new ArrayList<>(keys);
        Collections.reverse(reversedKeys);
        Map<RoadPlanKey, Long> reversed = deriveInOrder(reversedKeys);

        assertEquals(forward, reversed);
        assertEquals(
                PlanningSeed.derive(key(new PlanningRegion(-1, -3))),
                PlanningSeed.derive(key(new PlanningRegion(-1, -3))));
    }

    @Test
    void isolatesWorldDimensionRegionAndRulesIdentity() {
        RoadPlanKey base = key(new PlanningRegion(-1, -3));

        List<RoadPlanKey> identities = List.of(
                base,
                new RoadPlanKey(WORLD_SEED + 1, "minecraft:overworld", base.region(), "rules-v1"),
                new RoadPlanKey(WORLD_SEED, "minecraft:the_nether", base.region(), "rules-v1"),
                new RoadPlanKey(WORLD_SEED, "minecraft:overworld", new PlanningRegion(-1, -2), "rules-v1"),
                new RoadPlanKey(WORLD_SEED, "minecraft:overworld", base.region(), "rules-v2"));

        for (int index = 1; index < identities.size(); index++) {
            assertNotEquals(base, identities.get(index));
            assertNotEquals(PlanningSeed.derive(base), PlanningSeed.derive(identities.get(index)));
        }

        RegionalPlanCache<Long> cache = new RegionalPlanCache<>();
        identities.forEach(identity -> cache.getOrPlan(identity, PlanningSeed::derive));
        assertEquals(identities.size(), cache.size());

        assertThrows(IllegalArgumentException.class, () -> new RoadPlanKey(
                WORLD_SEED, " ", base.region(), "rules-v1"));
    }

    @Test
    void computesOnePlanForConcurrentRequestsAndCanInvalidateIt() throws Exception {
        RegionalPlanCache<String> cache = new RegionalPlanCache<>();
        RoadPlanKey key = key(new PlanningRegion(-1, -3));
        AtomicInteger computations = new AtomicInteger();
        CountDownLatch plannerEntered = new CountDownLatch(1);
        CountDownLatch releasePlanner = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Future<String>> futures = new ArrayList<>();
            for (int index = 0; index < 24; index++) {
                futures.add(executor.submit(() -> cache.getOrPlan(key, ignored -> {
                    computations.incrementAndGet();
                    plannerEntered.countDown();
                    await(releasePlanner);
                    return "plan-" + PlanningSeed.derive(key);
                })));
            }

            plannerEntered.await();
            releasePlanner.countDown();
            for (Future<String> future : futures) {
                assertEquals("plan-" + PlanningSeed.derive(key), future.get());
            }
        }

        assertEquals(1, computations.get());
        assertEquals(1, cache.size());

        cache.invalidateAll();
        assertEquals(0, cache.size());
        cache.getOrPlan(key, ignored -> {
            computations.incrementAndGet();
            return "replacement";
        });
        assertEquals(2, computations.get());
    }

    @Test
    void inFlightWorkCannotRepopulateAnInvalidatedCacheGeneration() throws Exception {
        RegionalPlanCache<String> cache = new RegionalPlanCache<>();
        RoadPlanKey key = key(new PlanningRegion(2, 3));
        CountDownLatch plannerEntered = new CountDownLatch(1);
        CountDownLatch releasePlanner = new CountDownLatch(1);

        try (var executor = Executors.newSingleThreadExecutor()) {
            Future<String> stale = executor.submit(() -> cache.getOrPlan(key, ignored -> {
                plannerEntered.countDown();
                await(releasePlanner);
                return "stale";
            }));

            plannerEntered.await();
            cache.invalidateAll();
            assertEquals("fresh", cache.getOrPlan(key, ignored -> "fresh"));
            releasePlanner.countDown();

            assertEquals("stale", stale.get());
            assertEquals("fresh", cache.getOrPlan(key, ignored -> "unexpected"));
            assertEquals(1, cache.size());
        }
    }

    private static RoadPlanKey key(PlanningRegion region) {
        return new RoadPlanKey(WORLD_SEED, "minecraft:overworld", region, "rules-v1");
    }

    private static Map<RoadPlanKey, Long> deriveInOrder(List<RoadPlanKey> keys) {
        Map<RoadPlanKey, Long> result = new LinkedHashMap<>();
        for (RoadPlanKey key : keys) {
            result.put(key, PlanningSeed.derive(key));
        }
        return result;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating planner test", exception);
        }
    }
}
