package net.austizz.lostcitiesroadfixes.integration;

import net.austizz.lostcitiesroadfixes.interchange.render.PlannedInterchangeGeometry;
import net.austizz.lostcitiesroadfixes.planning.RoadPlanKey;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record RegionalInterchangeGeometryPlan(
        RoadPlanKey key,
        List<PlannedInterchangeGeometry> interchanges,
        int rejectedCrossingCount) {
    public RegionalInterchangeGeometryPlan {
        Objects.requireNonNull(key, "key");
        interchanges = List.copyOf(Objects.requireNonNull(interchanges, "interchanges").stream()
                .sorted(Comparator.comparing(geometry -> geometry.plan().crossing()))
                .toList());
        if (rejectedCrossingCount < 0) {
            throw new IllegalArgumentException("Rejected crossing count cannot be negative");
        }
        for (PlannedInterchangeGeometry geometry : interchanges) {
            if (!key.region().owns(geometry.plan().crossing().chunk())) {
                throw new IllegalArgumentException(
                        "Interchange is not owned by plan region: "
                                + geometry.plan().crossing().chunk());
            }
        }
    }

    public List<PlannedInterchangeGeometry> affecting(ChunkPoint target) {
        Objects.requireNonNull(target, "target");
        return interchanges.stream()
                .filter(interchange -> interchange.mayAffect(target))
                .toList();
    }
}
