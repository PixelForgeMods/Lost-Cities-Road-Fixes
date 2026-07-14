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
import net.austizz.lostcitiesroadfixes.planning.RoadPlanKey;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
import net.austizz.lostcitiesroadfixes.render.ChunkRoadSurface;
import net.austizz.lostcitiesroadfixes.render.ElevatedRoadTile;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.PlanningRegion;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
        ChunkPoint endpointChunk = new ChunkPoint(0, 16);

        ChunkRoadSurface surface = new RuntimeRoadSurfaceComposer().compose(
                endpointChunk,
                List.of(tile(endpointChunk, RoadAxis.Z, 152)),
                List.of(geometry));

        assertTrue(surface.cellAt(8, 264, elevation(152)).isPresent());
        assertTrue(surface.cellAt(8, 265, elevation(152)).isPresent());
        assertTrue(surface.cellAt(8, 271, elevation(152)).isPresent());
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
    void regionalGeometryFiltersAtTheMaximumSixteenChunkEnvelope() {
        PlannedInterchangeGeometry geometry = geometry();
        RegionalInterchangeGeometryPlan plan = new RegionalInterchangeGeometryPlan(
                new RoadPlanKey(1L, "minecraft:overworld", new PlanningRegion(0, 0), "test"),
                List.of(geometry),
                0,
                0);

        assertEquals(List.of(geometry), plan.affecting(new ChunkPoint(16, 0)));
        assertTrue(plan.affecting(new ChunkPoint(17, 0)).isEmpty());
        assertTrue(geometry.mayAffect(new ChunkPoint(-16, -16)));
        assertFalse(geometry.mayAffect(new ChunkPoint(-17, -17)));
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
                256,
                128,
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

    private static HalfBlockElevation elevation(int halfBlocks) {
        return new HalfBlockElevation(halfBlocks);
    }
}
