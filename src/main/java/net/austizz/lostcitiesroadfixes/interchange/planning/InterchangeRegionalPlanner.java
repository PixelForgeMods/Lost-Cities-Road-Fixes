package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDecision;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeSelector;
import net.austizz.lostcitiesroadfixes.planning.RoadPlanKey;
import net.austizz.lostcitiesroadfixes.planning.continuity.ChunkBounds;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InterchangeRegionalPlanner {
    private final RoadCrossingSurveyor surveyor;
    private final InterchangeSelector selector;
    private final InterchangeConflictResolver conflictResolver;

    public InterchangeRegionalPlanner(
            RoadCrossingSurveyor surveyor,
            InterchangeSelector selector,
            InterchangeConflictResolver conflictResolver) {
        this.surveyor = Objects.requireNonNull(surveyor, "surveyor");
        this.selector = Objects.requireNonNull(selector, "selector");
        this.conflictResolver = Objects.requireNonNull(conflictResolver, "conflictResolver");
    }

    public RegionalInterchangePlan plan(
            RoadPlanKey key,
            RoadTileLookup roads,
            CrossingElevationModel elevations) {
        ChunkBounds ownership = new ChunkBounds(
                key.region().ownershipMinChunkX(),
                key.region().ownershipMaxChunkX(),
                key.region().ownershipMinChunkZ(),
                key.region().ownershipMaxChunkZ());
        return plan(key, ownership, roads, elevations);
    }

    public RegionalInterchangePlan plan(
            RoadPlanKey key,
            ChunkBounds bounds,
            RoadTileLookup roads,
            CrossingElevationModel elevations) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(roads, "roads");
        Objects.requireNonNull(elevations, "elevations");
        List<PlannedInterchange> candidates = new ArrayList<>();
        List<RejectedRoadCrossing> rejected = new ArrayList<>();

        ChunkBounds surveyBounds = conflictResolver.surveyBounds(bounds);
        for (int z = surveyBounds.minZ(); z <= surveyBounds.maxZ(); z++) {
            for (int x = surveyBounds.minX(); x <= surveyBounds.maxX(); x++) {
                ChunkPoint chunk = new ChunkPoint(x, z);
                surveyor.survey(chunk, roads, elevations, key.worldSeed()).ifPresent(crossing -> {
                    InterchangeDecision decision = selector.select(crossing.selectionSite());
                    if (decision.selected().isPresent()) {
                        candidates.add(new PlannedInterchange(crossing, decision));
                    } else if (ownsOutput(key, bounds, chunk)) {
                        rejected.add(new RejectedRoadCrossing(crossing, decision));
                    }
                });
            }
        }
        InterchangeConflictResolution resolution = conflictResolver.resolve(candidates);
        List<PlannedInterchange> selected = resolution.interchanges().stream()
                .filter(interchange -> ownsOutput(key, bounds, interchange.crossing().chunk()))
                .toList();
        List<ConflictedRoadCrossing> conflicts = resolution.conflicts().stream()
                .filter(conflict -> ownsOutput(key, bounds, conflict.crossing().chunk()))
                .toList();
        return new RegionalInterchangePlan(key, selected, rejected, conflicts);
    }

    private static boolean ownsOutput(
            RoadPlanKey key,
            ChunkBounds bounds,
            ChunkPoint chunk) {
        return bounds.contains(chunk) && key.region().owns(chunk);
    }
}
