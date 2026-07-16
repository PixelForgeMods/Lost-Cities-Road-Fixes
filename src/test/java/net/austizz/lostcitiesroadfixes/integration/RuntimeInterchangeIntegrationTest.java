package net.austizz.lostcitiesroadfixes.integration;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDecision;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeSelector;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.TrafficDemand;
import net.austizz.lostcitiesroadfixes.interchange.layout.ApproachDirection;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeLayoutFactory;
import net.austizz.lostcitiesroadfixes.interchange.planning.CrossingDecks;
import net.austizz.lostcitiesroadfixes.interchange.planning.DetectedRoadCrossing;
import net.austizz.lostcitiesroadfixes.interchange.planning.PlannedInterchange;
import net.austizz.lostcitiesroadfixes.interchange.render.InterchangeGeometryPlanner;
import net.austizz.lostcitiesroadfixes.interchange.render.PlannedInterchangeGeometry;
import net.austizz.lostcitiesroadfixes.diagnostics.InterchangeExplanation;
import net.austizz.lostcitiesroadfixes.planning.RoadPlanKey;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
import net.austizz.lostcitiesroadfixes.render.ChunkRoadSurface;
import net.austizz.lostcitiesroadfixes.render.ElevatedRoadTile;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceCell;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.PlanningRegion;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeInterchangeIntegrationTest {
    private static final ChunkPoint CENTER = new ChunkPoint(0, 0);

    @Test
    void replacesNativeCrossingTilesWithTheRegradedStackedSurface() {
        PlannedInterchangeGeometry geometry = geometry();
        List<ElevatedRoadTile> nativeRoads = List.of(
                tile(CENTER, RoadAxis.X, 140),
                tile(CENTER, RoadAxis.Z, 152));

        ChunkRoadSurface surface = new RuntimeRoadSurfaceComposer().compose(
                CENTER, nativeRoads, List.of(geometry));

        assertTrue(surface.cellAt(8, 8, elevation(140)).isPresent());
        assertTrue(surface.cellAt(8, 8, elevation(160)).isPresent());
        assertFalse(surface.cellAt(8, 8, elevation(152)).isPresent());
    }

    @Test
    void pipelineWritesExactlyOneDeterministicComposedSurface() {
        PlannedInterchangeGeometry geometry = geometry();
        List<ElevatedRoadTile> nativeRoads = List.of(
                tile(CENTER, RoadAxis.Z, 152),
                tile(CENTER, RoadAxis.X, 140));
        AtomicInteger writes = new AtomicInteger();
        AtomicReference<ChunkRoadSurface> written = new AtomicReference<>();

        new RuntimeRoadRenderPipeline().render(
                CENTER,
                nativeRoads,
                List.of(geometry),
                surface -> {
                    writes.incrementAndGet();
                    written.set(surface);
                });

        assertEquals(1, writes.get());
        ChunkRoadSurface expected = new RuntimeRoadSurfaceComposer().compose(
                CENTER, nativeRoads.reversed(), List.of(geometry));
        assertEquals(expected.chunk(), written.get().chunk());
        assertEquals(expected.cells(), written.get().cells());
    }

    @Test
    void nativeRoadResumesImmediatelyBeyondAnEnvelopePortInTheSameChunk() {
        PlannedInterchangeGeometry geometry = geometry();
        ChunkPoint endpointChunk = new ChunkPoint(0, 20);

        ChunkRoadSurface surface = new RuntimeRoadSurfaceComposer().compose(
                endpointChunk,
                List.of(tile(endpointChunk, RoadAxis.Z, 152)),
                List.of(geometry));

        assertTrue(surface.cellAt(8, 328, elevation(152)).isPresent());
        assertTrue(surface.cellAt(8, 329, elevation(152)).isPresent());
        assertTrue(surface.cellAt(8, 335, elevation(152)).isPresent());
    }

    @Test
    void incompatibleNativeIntersectionDecksAreRemovedInsideAnInterchange() {
        PlannedInterchangeGeometry geometry = geometry();
        List<ElevatedRoadTile> conflictingIntersection = List.of(
                tile(CENTER, RoadAxis.X, 146),
                tile(CENTER, RoadAxis.Z, 146));

        ChunkRoadSurface surface = new RuntimeRoadSurfaceComposer().compose(
                CENTER, conflictingIntersection, List.of(geometry));

        Map<String, List<RoadSurfaceCell>> byColumn = surface.cells().stream()
                .collect(Collectors.groupingBy(cell ->
                        cell.position().x() + "," + cell.position().z()));
        List<String> unsafeColumns = byColumn.entrySet().stream()
                .filter(entry -> hasUnsafeVerticalPair(entry.getValue()))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        assertTrue(unsafeColumns.isEmpty(),
                () -> "intersection decks conflict with interchange clearance: "
                        + unsafeColumns);
    }

    @Test
    void compactDiamondSharedTurnsCannotAbortHighwayChunkComposition() {
        DetectedRoadCrossing crossing = new DetectedRoadCrossing(
                CENTER,
                JunctionForm.FOUR_WAY,
                0,
                1,
                EnumSet.allOf(ApproachDirection.class),
                320,
                64,
                2,
                TrafficDemand.REGIONAL,
                2,
                false,
                false,
                new CrossingDecks(
                        elevation(142),
                        elevation(154),
                        elevation(142),
                        elevation(162)),
                1L);
        InterchangeDecision decision = InterchangeSelector.withBuiltIns()
                .select(crossing.selectionSite());
        assertEquals(
                net.austizz.lostcitiesroadfixes.interchange.InterchangeType.DIAMOND,
                decision.selected().orElseThrow().type(),
                decision::diagnostic);
        PlannedInterchangeGeometry geometry = new InterchangeGeometryPlanner(
                new InterchangeLayoutFactory(RoadDesignStandard.DEFAULT))
                .create(new PlannedInterchange(crossing, decision));

        ChunkRoadSurface surface = assertDoesNotThrow(() ->
                new RuntimeRoadSurfaceComposer().compose(
                        new ChunkPoint(-5, -2),
                        List.of(),
                        List.of(geometry)));

        assertTrue(surface.cells().stream().anyMatch(cell ->
                cell.position().x() == -68 && cell.position().z() == -28));
    }

    @Test
    void straightThroughFallbackKeepsBothSafelyGradedMainlines() {
        PlannedInterchangeGeometry geometry = geometry();

        ChunkRoadSurface surface = new RuntimeRoadSurfaceComposer()
                .composeStraightThrough(
                        CENTER,
                        List.of(
                                tile(CENTER, RoadAxis.X, 140),
                                tile(CENTER, RoadAxis.Z, 152)),
                        List.of(geometry));

        assertTrue(surface.cellAt(8, 8, elevation(140)).isPresent());
        assertTrue(surface.cellAt(8, 8, elevation(160)).isPresent());
        assertFalse(surface.cellAt(8, 8, elevation(152)).isPresent());
    }

    @Test
    void ownerWindowIncludesPositiveAndNegativeBoundaryRegions() {
        assertEquals(List.of(
                        new PlanningRegion(0, -1),
                        new PlanningRegion(1, -1),
                        new PlanningRegion(0, 0),
                        new PlanningRegion(1, 0)),
                InterchangeRegionWindow.ownerRegionsAffecting(
                        new ChunkPoint(63, 0), 256));
        assertEquals(List.of(
                        new PlanningRegion(-2, -2),
                        new PlanningRegion(-1, -2),
                        new PlanningRegion(-2, -1),
                        new PlanningRegion(-1, -1)),
                InterchangeRegionWindow.ownerRegionsAffecting(
                        new ChunkPoint(-64, -64), 256));
    }

    @Test
    void regionalGeometryFiltersAtTheMaximumTwentyChunkEnvelope() {
        PlannedInterchangeGeometry geometry = geometry();
        RegionalInterchangeGeometryPlan plan = new RegionalInterchangeGeometryPlan(
                new RoadPlanKey(1L, "minecraft:overworld", new PlanningRegion(0, 0), "test"),
                List.of(geometry),
                0,
                0);

        assertEquals(List.of(geometry), plan.affecting(new ChunkPoint(20, 0)));
        assertTrue(plan.affecting(new ChunkPoint(21, 0)).isEmpty());
        assertTrue(geometry.mayAffect(new ChunkPoint(-20, -20)));
        assertFalse(geometry.mayAffect(new ChunkPoint(-21, -21)));
    }

    @Test
    void regionalPlanRetainsAnExplanationForEachSurveyedCrossing() {
        ChunkPoint explainedChunk = new ChunkPoint(3, 4);
        InterchangeExplanation explanation = InterchangeExplanation.none(explainedChunk);
        RegionalInterchangeGeometryPlan plan = new RegionalInterchangeGeometryPlan(
                new RoadPlanKey(1L, "minecraft:overworld", new PlanningRegion(0, 0), "test"),
                List.of(),
                0,
                0,
                List.of(explanation));

        assertEquals(explanation, plan.explanationAt(explainedChunk).orElseThrow());
        assertTrue(plan.explanationAt(new ChunkPoint(4, 4)).isEmpty());
    }

    @Test
    void invalidationReplacesBothRuntimePlanGenerations() {
        RuntimePlanCaches<String, String> caches = new RuntimePlanCaches<>();
        RoadPlanKey key = new RoadPlanKey(
                1L, "minecraft:overworld", new PlanningRegion(0, 0), "test");
        AtomicInteger roadPlans = new AtomicInteger();
        AtomicInteger interchangePlans = new AtomicInteger();

        assertEquals("roads-1", caches.roadPlan(
                key, ignored -> "roads-" + roadPlans.incrementAndGet()));
        assertEquals("interchanges-1", caches.interchangePlan(
                key, ignored -> "interchanges-" + interchangePlans.incrementAndGet()));
        caches.invalidateAll();
        assertEquals(0, caches.roadSize());
        assertEquals(0, caches.interchangeSize());
        assertEquals("roads-2", caches.roadPlan(
                key, ignored -> "roads-" + roadPlans.incrementAndGet()));
        assertEquals("interchanges-2", caches.interchangePlan(
                key, ignored -> "interchanges-" + interchangePlans.incrementAndGet()));
    }

    private static PlannedInterchangeGeometry geometry() {
        DetectedRoadCrossing crossing = new DetectedRoadCrossing(
                CENTER,
                JunctionForm.FOUR_WAY,
                0,
                1,
                EnumSet.allOf(ApproachDirection.class),
                320,
                240,
                4,
                TrafficDemand.HIGH,
                4,
                true,
                true,
                new CrossingDecks(
                        elevation(140), elevation(152), elevation(140), elevation(160)),
                0x5eedL);
        InterchangeDecision decision = InterchangeSelector.withBuiltIns()
                .select(crossing.selectionSite());
        PlannedInterchange selected = new PlannedInterchange(crossing, decision);
        return new InterchangeGeometryPlanner(
                new InterchangeLayoutFactory(RoadDesignStandard.DEFAULT)).create(selected);
    }

    private static ElevatedRoadTile tile(
            ChunkPoint chunk,
            RoadAxis axis,
            int halfBlocks) {
        return new ElevatedRoadTile(chunk, axis, elevation(halfBlocks));
    }

    private static boolean hasUnsafeVerticalPair(List<RoadSurfaceCell> column) {
        int minimumClearance = RoadDesignStandard.DEFAULT
                .minimumVehicleClearanceBlocks() * 2;
        for (int left = 0; left < column.size(); left++) {
            for (int right = left + 1; right < column.size(); right++) {
                int separation = StrictMath.abs(
                        column.get(left).position().elevation().halfBlocks()
                                - column.get(right).position().elevation().halfBlocks());
                if (separation > 0 && separation <= minimumClearance) {
                    return true;
                }
            }
        }
        return false;
    }

    private static HalfBlockElevation elevation(int halfBlocks) {
        return new HalfBlockElevation(halfBlocks);
    }
}
