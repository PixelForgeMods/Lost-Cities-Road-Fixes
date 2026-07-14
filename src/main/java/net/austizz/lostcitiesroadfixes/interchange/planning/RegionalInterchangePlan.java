package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.planning.RoadPlanKey;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record RegionalInterchangePlan(
        RoadPlanKey key,
        List<PlannedInterchange> interchanges,
        List<RejectedRoadCrossing> rejectedCrossings,
        List<ConflictedRoadCrossing> conflictedCrossings) {
    public RegionalInterchangePlan {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(interchanges, "interchanges");
        Objects.requireNonNull(rejectedCrossings, "rejectedCrossings");
        Objects.requireNonNull(conflictedCrossings, "conflictedCrossings");
        interchanges = List.copyOf(interchanges.stream().sorted().toList());
        rejectedCrossings = List.copyOf(rejectedCrossings.stream().sorted().toList());
        conflictedCrossings = List.copyOf(conflictedCrossings.stream().sorted().toList());
        Set<ChunkPoint> chunks = new HashSet<>();
        interchanges.forEach(interchange -> {
            if (!chunks.add(interchange.crossing().chunk())) {
                throw new IllegalArgumentException(
                        "Duplicate crossing " + interchange.crossing().chunk());
            }
        });
        rejectedCrossings.forEach(rejected -> {
            if (!chunks.add(rejected.crossing().chunk())) {
                throw new IllegalArgumentException(
                        "Duplicate crossing " + rejected.crossing().chunk());
            }
        });
        conflictedCrossings.forEach(conflict -> {
            if (!chunks.add(conflict.crossing().chunk())) {
                throw new IllegalArgumentException(
                        "Duplicate crossing " + conflict.crossing().chunk());
            }
        });
    }
}
