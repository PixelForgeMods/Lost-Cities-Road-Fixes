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

    public InterchangeRegionalPlanner(
            RoadCrossingSurveyor surveyor,
            InterchangeSelector selector) {
        this.surveyor = Objects.requireNonNull(surveyor, "surveyor");
        this.selector = Objects.requireNonNull(selector, "selector");
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
        List<PlannedInterchange> selected = new ArrayList<>();
        List<RejectedRoadCrossing> rejected = new ArrayList<>();

        for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                ChunkPoint chunk = new ChunkPoint(x, z);
                if (!key.region().owns(chunk)) {
                    continue;
                }
                surveyor.survey(chunk, roads, elevations, key.worldSeed()).ifPresent(crossing -> {
                    InterchangeDecision decision = selector.select(crossing.selectionSite());
                    if (decision.selected().isPresent()) {
                        selected.add(new PlannedInterchange(crossing, decision));
                    } else {
                        rejected.add(new RejectedRoadCrossing(crossing, decision));
                    }
                });
            }
        }
        return new RegionalInterchangePlan(key, selected, rejected);
    }
}
