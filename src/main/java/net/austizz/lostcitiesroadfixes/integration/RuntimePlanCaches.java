package net.austizz.lostcitiesroadfixes.integration;

import net.austizz.lostcitiesroadfixes.planning.RegionalPlanCache;
import net.austizz.lostcitiesroadfixes.planning.RoadPlanKey;

import java.util.function.Function;

final class RuntimePlanCaches<R, I> {
    private final RegionalPlanCache<R> roads = new RegionalPlanCache<>();
    private final RegionalPlanCache<I> interchanges = new RegionalPlanCache<>();

    R roadPlan(RoadPlanKey key, Function<RoadPlanKey, R> planner) {
        return roads.getOrPlan(key, planner);
    }

    I interchangePlan(RoadPlanKey key, Function<RoadPlanKey, I> planner) {
        return interchanges.getOrPlan(key, planner);
    }

    void invalidateAll() {
        roads.invalidateAll();
        interchanges.invalidateAll();
    }

    int roadSize() {
        return roads.size();
    }

    int interchangeSize() {
        return interchanges.size();
    }
}
