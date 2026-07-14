package net.austizz.lostcitiesroadfixes.planning;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class RegionalPlanCache<P> {
    private final AtomicReference<Generation<P>> active =
            new AtomicReference<>(new Generation<>());

    public P getOrPlan(RoadPlanKey key, Function<RoadPlanKey, P> planner) {
        return getOrPlan(key, Integer.MAX_VALUE, planner);
    }

    public P getOrPlan(
            RoadPlanKey key,
            int maximumEntries,
            Function<RoadPlanKey, P> planner) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(planner, "planner");
        if (maximumEntries < 1) {
            throw new IllegalArgumentException("Maximum cache entries must be positive");
        }
        Generation<P> generation = active.get();
        P result = generation.plans.computeIfAbsent(key, candidate ->
                Objects.requireNonNull(planner.apply(candidate), "planner returned null"));
        generation.recordAndTrim(key, maximumEntries);
        return result;
    }

    public void invalidateAll() {
        active.set(new Generation<>());
    }

    public int size() {
        return active.get().plans.size();
    }

    private static final class Generation<P> {
        private final ConcurrentMap<RoadPlanKey, P> plans = new ConcurrentHashMap<>();
        private final Object evictionLock = new Object();
        private final ArrayDeque<RoadPlanKey> insertionOrder = new ArrayDeque<>();
        private final Set<RoadPlanKey> recorded = new HashSet<>();

        private void recordAndTrim(RoadPlanKey key, int maximumEntries) {
            synchronized (evictionLock) {
                if (plans.containsKey(key) && recorded.add(key)) {
                    insertionOrder.addLast(key);
                }
                while (plans.size() > maximumEntries) {
                    RoadPlanKey oldest = insertionOrder.pollFirst();
                    if (oldest == null) {
                        break;
                    }
                    recorded.remove(oldest);
                    plans.remove(oldest);
                }
            }
        }
    }
}
