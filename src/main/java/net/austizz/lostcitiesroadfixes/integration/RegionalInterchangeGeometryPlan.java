package net.austizz.lostcitiesroadfixes.integration;

import net.austizz.lostcitiesroadfixes.diagnostics.InterchangeExplanation;
import net.austizz.lostcitiesroadfixes.interchange.render.PlannedInterchangeGeometry;
import net.austizz.lostcitiesroadfixes.planning.RoadPlanKey;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RegionalInterchangeGeometryPlan(
        RoadPlanKey key,
        List<PlannedInterchangeGeometry> interchanges,
        int rejectedCrossingCount,
        int conflictedCrossingCount,
        List<InterchangeExplanation> explanations) {
    public RegionalInterchangeGeometryPlan {
        Objects.requireNonNull(key, "key");
        interchanges = List.copyOf(Objects.requireNonNull(interchanges, "interchanges").stream()
                .sorted(Comparator.comparing(geometry -> geometry.plan().crossing()))
                .toList());
        if (rejectedCrossingCount < 0) {
            throw new IllegalArgumentException("Rejected crossing count cannot be negative");
        }
        if (conflictedCrossingCount < 0) {
            throw new IllegalArgumentException("Conflicted crossing count cannot be negative");
        }
        explanations = List.copyOf(Objects.requireNonNull(explanations, "explanations").stream()
                .sorted(Comparator
                        .comparingInt((InterchangeExplanation explanation) ->
                                explanation.chunk().z())
                        .thenComparingInt(explanation -> explanation.chunk().x()))
                .toList());
        long distinctExplanationChunks = explanations.stream()
                .map(InterchangeExplanation::chunk)
                .distinct()
                .count();
        if (distinctExplanationChunks != explanations.size()) {
            throw new IllegalArgumentException("Duplicate interchange explanation chunk");
        }
        for (PlannedInterchangeGeometry geometry : interchanges) {
            if (!key.region().owns(geometry.plan().crossing().chunk())) {
                throw new IllegalArgumentException(
                        "Interchange is not owned by plan region: "
                                + geometry.plan().crossing().chunk());
            }
        }
    }

    public RegionalInterchangeGeometryPlan(
            RoadPlanKey key,
            List<PlannedInterchangeGeometry> interchanges,
            int rejectedCrossingCount,
            int conflictedCrossingCount) {
        this(
                key,
                interchanges,
                rejectedCrossingCount,
                conflictedCrossingCount,
                List.of());
    }

    public List<PlannedInterchangeGeometry> affecting(ChunkPoint target) {
        Objects.requireNonNull(target, "target");
        return interchanges.stream()
                .filter(interchange -> interchange.mayAffect(target))
                .toList();
    }

    public Optional<InterchangeExplanation> explanationAt(ChunkPoint chunk) {
        Objects.requireNonNull(chunk, "chunk");
        return explanations.stream()
                .filter(explanation -> explanation.chunk().equals(chunk))
                .findFirst();
    }
}
