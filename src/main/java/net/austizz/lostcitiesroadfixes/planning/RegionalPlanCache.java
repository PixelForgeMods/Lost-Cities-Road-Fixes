package net.austizz.lostcitiesroadfixes.planning;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class RegionalPlanCache<P> {
    private final AtomicReference<ConcurrentMap<RoadPlanKey, P>> active =
            new AtomicReference<>(new ConcurrentHashMap<>());

    public P getOrPlan(RoadPlanKey key, Function<RoadPlanKey, P> planner) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(planner, "planner");
        ConcurrentMap<RoadPlanKey, P> generation = active.get();
        return generation.computeIfAbsent(key, candidate ->
                Objects.requireNonNull(planner.apply(candidate), "planner returned null"));
    }

    public void invalidateAll() {
        active.set(new ConcurrentHashMap<>());
    }

    public int size() {
        return active.get().size();
    }
}
