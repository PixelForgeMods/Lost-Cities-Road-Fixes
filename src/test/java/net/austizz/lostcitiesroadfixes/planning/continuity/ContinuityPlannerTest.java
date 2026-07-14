package net.austizz.lostcitiesroadfixes.planning.continuity;

import net.austizz.lostcitiesroadfixes.planning.RoadPlanKey;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.PlanningRegion;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContinuityPlannerTest {
    private final ContinuityPlanner planner = new ContinuityPlanner(4);

    @Test
    void repairsTheLiteralOneChunkGapWithoutReplacingObservedTiles() {
        Map<RoadTileKey, Integer> observations = Map.of(
                tile(0, 0, RoadAxis.X), 0,
                tile(2, 0, RoadAxis.X), 0);

        RegionalRoadPlan plan = planner.plan(
                key(new PlanningRegion(0, 0)),
                new ChunkBounds(0, 2, 0, 0),
                source(observations));

        assertEquals(RoadTileOrigin.OBSERVED, plan.tileAt(new ChunkPoint(0, 0), RoadAxis.X)
                .orElseThrow().origin());
        RoadTile repair = plan.tileAt(new ChunkPoint(1, 0), RoadAxis.X).orElseThrow();
        assertEquals(0, repair.level());
        assertEquals(RoadTileOrigin.CONTINUITY_REPAIR, repair.origin());
        assertEquals(RoadTileOrigin.OBSERVED, plan.tileAt(new ChunkPoint(2, 0), RoadAxis.X)
                .orElseThrow().origin());
    }

    @Test
    void repairsBothAxesButRespectsGapAndLevelLimits() {
        Map<RoadTileKey, Integer> observations = new HashMap<>();
        observations.put(tile(0, 0, RoadAxis.X), 0);
        observations.put(tile(5, 0, RoadAxis.X), 0); // Four missing chunks: repair.
        observations.put(tile(0, 1, RoadAxis.X), 0);
        observations.put(tile(6, 1, RoadAxis.X), 0); // Five missing chunks: leave open.
        observations.put(tile(0, 2, RoadAxis.X), 0);
        observations.put(tile(2, 2, RoadAxis.X), 1); // Incompatible levels: leave open.
        observations.put(tile(7, 0, RoadAxis.Z), 1);
        observations.put(tile(7, 2, RoadAxis.Z), 1); // One missing Z chunk: repair.

        RegionalRoadPlan plan = planner.plan(
                key(new PlanningRegion(0, 0)),
                new ChunkBounds(0, 7, 0, 6),
                source(observations));

        for (int x = 1; x <= 4; x++) {
            assertTrue(plan.tileAt(new ChunkPoint(x, 0), RoadAxis.X).isPresent());
        }
        assertFalse(plan.tileAt(new ChunkPoint(3, 1), RoadAxis.X).isPresent());
        assertFalse(plan.tileAt(new ChunkPoint(1, 2), RoadAxis.X).isPresent());
        assertEquals(1, plan.tileAt(new ChunkPoint(7, 1), RoadAxis.Z).orElseThrow().level());
    }

    @Test
    void samplesEveryChunkAxisOnceAndReturnsSortedImmutableTiles() {
        AtomicInteger samples = new AtomicInteger();
        RoadObservationSource source = (chunk, axis) -> {
            samples.incrementAndGet();
            return chunk.equals(new ChunkPoint(1, 1)) ? OptionalInt.of(0) : OptionalInt.empty();
        };

        RegionalRoadPlan plan = planner.plan(
                key(new PlanningRegion(0, 0)),
                new ChunkBounds(0, 2, 0, 1),
                source);

        assertEquals(3 * 2 * 2, samples.get());
        List<RoadTile> sorted = new ArrayList<>(plan.tiles());
        sorted.sort(RoadTile.ORDER);
        assertEquals(sorted, plan.tiles());
        assertThrows(UnsupportedOperationException.class, () -> plan.tiles().add(sorted.getFirst()));
    }

    @Test
    void adjacentRegionsJoinAcrossTheirOwnershipBoundary() {
        Map<RoadTileKey, Integer> observations = Map.of(
                tile(62, 10, RoadAxis.X), 0,
                tile(67, 10, RoadAxis.X), 0);
        RoadObservationSource source = source(observations);

        RegionalRoadPlan west = planner.plan(key(new PlanningRegion(0, 0)), source);
        RegionalRoadPlan east = planner.plan(key(new PlanningRegion(1, 0)), source);

        RoadTile westEdge = west.tileAt(new ChunkPoint(63, 10), RoadAxis.X).orElseThrow();
        RoadTile eastEdge = east.tileAt(new ChunkPoint(64, 10), RoadAxis.X).orElseThrow();
        assertEquals(RoadTileOrigin.CONTINUITY_REPAIR, westEdge.origin());
        assertEquals(RoadTileOrigin.CONTINUITY_REPAIR, eastEdge.origin());
        assertEquals(westEdge.level(), eastEdge.level());
    }

    private static RoadPlanKey key(PlanningRegion region) {
        return new RoadPlanKey(42L, "minecraft:overworld", region, "rules-v1");
    }

    private static RoadTileKey tile(int x, int z, RoadAxis axis) {
        return new RoadTileKey(new ChunkPoint(x, z), axis);
    }

    private static RoadObservationSource source(Map<RoadTileKey, Integer> observations) {
        return (chunk, axis) -> {
            Integer level = observations.get(new RoadTileKey(chunk, axis));
            return level == null ? OptionalInt.empty() : OptionalInt.of(level);
        };
    }
}
