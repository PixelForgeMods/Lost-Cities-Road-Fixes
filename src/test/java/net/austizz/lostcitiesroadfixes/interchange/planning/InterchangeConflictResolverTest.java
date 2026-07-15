package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDecision;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeSelector;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.TrafficDemand;
import net.austizz.lostcitiesroadfixes.interchange.layout.ApproachDirection;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeLayoutFactory;
import net.austizz.lostcitiesroadfixes.interchange.render.InterchangeGeometryPlanner;
import net.austizz.lostcitiesroadfixes.interchange.render.PlannedInterchangeGeometry;
import net.austizz.lostcitiesroadfixes.integration.RuntimeRoadSurfaceComposer;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterchangeConflictResolverTest {
    private static final InterchangeConflictResolver RESOLVER =
            new InterchangeConflictResolver(RoadDesignStandard.DEFAULT);

    @Test
    void suppressesTheLowerPriorityCoreAtEightChunkSpacing() {
        PlannedInterchange winner = candidate(new ChunkPoint(0, 0), 1L);
        PlannedInterchange blocked = candidate(new ChunkPoint(8, 0), 2L);

        InterchangeConflictResolution resolution = RESOLVER.resolve(List.of(blocked, winner));

        assertEquals(List.of(winner), resolution.interchanges());
        assertEquals(1, resolution.conflicts().size());
        ConflictedRoadCrossing conflict = resolution.conflicts().getFirst();
        assertEquals(blocked.crossing(), conflict.crossing());
        assertEquals(winner.crossing().chunk(), conflict.blockingCrossing());
        assertEquals(224, conflict.minimumCenterSeparationBlocks());
        assertTrue(conflict.diagnostic().contains("chunk 0,0"));
    }

    @Test
    void exactCoreTangencyDoesNotCreateAConflict() {
        PlannedInterchange first = candidate(new ChunkPoint(0, 0), 1L);
        PlannedInterchange tangent = candidate(new ChunkPoint(16, 0), 2L);

        InterchangeConflictResolution resolution = RESOLVER.resolve(List.of(first, tangent));

        assertEquals(List.of(first, tangent), resolution.interchanges());
        assertTrue(resolution.conflicts().isEmpty());
        assertFalse(RESOLVER.conflicts(first, tangent));
    }

    @Test
    void resolutionDoesNotDependOnCandidateInsertionOrder() {
        List<PlannedInterchange> candidates = List.of(
                candidate(new ChunkPoint(0, 0), 9L),
                candidate(new ChunkPoint(8, 0), 3L),
                candidate(new ChunkPoint(16, 0), 6L));

        assertEquals(
                RESOLVER.resolve(candidates),
                RESOLVER.resolve(candidates.reversed()));
    }

    @Test
    void equalPriorityConflictWinnerDoesNotDependOnSelectionSeed() {
        PlannedInterchange lowerCoordinateWithHighSeed = candidate(
                new ChunkPoint(0, 0), Long.MAX_VALUE);
        PlannedInterchange higherCoordinateWithLowSeed = candidate(
                new ChunkPoint(8, 0), 0L);
        PlannedInterchange lowerCoordinateWithLowSeed = candidate(
                new ChunkPoint(0, 0), 0L);
        PlannedInterchange higherCoordinateWithHighSeed = candidate(
                new ChunkPoint(8, 0), Long.MAX_VALUE);

        InterchangeConflictResolution first = RESOLVER.resolve(List.of(
                higherCoordinateWithLowSeed,
                lowerCoordinateWithHighSeed));
        InterchangeConflictResolution second = RESOLVER.resolve(List.of(
                higherCoordinateWithHighSeed,
                lowerCoordinateWithLowSeed));

        assertEquals(new ChunkPoint(0, 0), first.interchanges().getFirst().crossing().chunk());
        assertEquals(new ChunkPoint(0, 0), second.interchanges().getFirst().crossing().chunk());
    }

    @Test
    void higherDemandWinsBeforeTheCoordinateTieBreaker() {
        PlannedInterchange highDemand = candidate(
                new ChunkPoint(0, 0), 99L, TrafficDemand.HIGH, true);
        PlannedInterchange regionalDemand = candidate(
                new ChunkPoint(8, 0), 1L, TrafficDemand.REGIONAL, false);

        InterchangeConflictResolution resolution = RESOLVER.resolve(
                List.of(regionalDemand, highDemand));

        assertEquals(List.of(highDemand), resolution.interchanges());
        assertEquals(highDemand.crossing().chunk(),
                resolution.conflicts().getFirst().blockingCrossing());
    }

    @Test
    void localPriorityMinimaCannotConflictAcrossAChain() {
        PlannedInterchange first = candidate(new ChunkPoint(0, 0), 1L);
        PlannedInterchange middle = candidate(
                new ChunkPoint(8, 0), 3L, TrafficDemand.REGIONAL, false);
        PlannedInterchange last = candidate(new ChunkPoint(16, 0), 2L);

        InterchangeConflictResolution resolution = RESOLVER.resolve(
                List.of(middle, last, first));

        assertEquals(List.of(first, last), resolution.interchanges());
        assertEquals(List.of(middle.crossing()), resolution.conflicts().stream()
                .map(ConflictedRoadCrossing::crossing)
                .toList());
        assertFalse(RESOLVER.conflicts(
                resolution.interchanges().get(0),
                resolution.interchanges().get(1)));
    }

    @Test
    void surveyHaloCoversTheLargestCompiledApproachPair() {
        assertEquals(64, RESOLVER.surveyMarginChunks());
    }

    @Test
    void rejectsOverlappingCompiledDiamondsWithIncompatibleSharedArterialGrades() {
        PlannedInterchange raised = diamond(
                new ChunkPoint(0, 0),
                new CrossingDecks(
                        new HalfBlockElevation(152),
                        new HalfBlockElevation(140),
                        new HalfBlockElevation(160),
                        new HalfBlockElevation(140)));
        PlannedInterchange nativeHeight = diamond(
                new ChunkPoint(10, 0),
                new CrossingDecks(
                        new HalfBlockElevation(152),
                        new HalfBlockElevation(164),
                        new HalfBlockElevation(152),
                        new HalfBlockElevation(172)));

        InterchangeConflictResolution resolution = RESOLVER.resolve(
                List.of(nativeHeight, raised));

        assertEquals(1, resolution.interchanges().size());
        assertEquals(1, resolution.conflicts().size());
        assertTrue(RESOLVER.conflicts(raised, nativeHeight));
    }

    @Test
    void permitsOverlappingCompiledCorridorsWhenTheSharedGradeIsIdentical() {
        CrossingDecks decks = new CrossingDecks(
                new HalfBlockElevation(152),
                new HalfBlockElevation(164),
                new HalfBlockElevation(152),
                new HalfBlockElevation(172));
        PlannedInterchange first = diamond(new ChunkPoint(0, 0), decks);
        PlannedInterchange second = diamond(new ChunkPoint(10, 0), decks);

        InterchangeConflictResolution resolution = RESOLVER.resolve(List.of(first, second));

        assertEquals(2, resolution.interchanges().size());
        assertTrue(resolution.conflicts().isEmpty());
    }

    @Test
    void conflictResolutionRemovesTheUnsafeSharedCorridorBeforeComposition() {
        PlannedInterchange raised = diamond(
                new ChunkPoint(0, 0),
                new CrossingDecks(
                        new HalfBlockElevation(152),
                        new HalfBlockElevation(140),
                        new HalfBlockElevation(160),
                        new HalfBlockElevation(140)));
        PlannedInterchange nativeHeight = diamond(
                new ChunkPoint(10, 0),
                new CrossingDecks(
                        new HalfBlockElevation(152),
                        new HalfBlockElevation(164),
                        new HalfBlockElevation(152),
                        new HalfBlockElevation(172)));
        InterchangeGeometryPlanner geometryPlanner = new InterchangeGeometryPlanner(
                new InterchangeLayoutFactory(RoadDesignStandard.DEFAULT));
        RuntimeRoadSurfaceComposer composer = new RuntimeRoadSurfaceComposer();
        ChunkPoint overlapChunk = new ChunkPoint(5, 0);

        List<PlannedInterchangeGeometry> unresolved = List.of(
                geometryPlanner.create(raised),
                geometryPlanner.create(nativeHeight));
        assertThrows(
                IllegalArgumentException.class,
                () -> composer.compose(overlapChunk, List.of(), unresolved));

        InterchangeConflictResolution resolution = RESOLVER.resolve(
                List.of(nativeHeight, raised));
        List<PlannedInterchangeGeometry> selected = resolution.interchanges().stream()
                .map(geometryPlanner::create)
                .toList();
        assertDoesNotThrow(
                () -> composer.compose(overlapChunk, List.of(), selected));
        assertEquals(1, resolution.conflicts().size());
    }

    private static PlannedInterchange candidate(ChunkPoint chunk, long seed) {
        return candidate(chunk, seed, TrafficDemand.HIGH, true);
    }

    private static PlannedInterchange candidate(
            ChunkPoint chunk,
            long seed,
            TrafficDemand demand,
            boolean requireFreeFlow) {
        DetectedRoadCrossing crossing = new DetectedRoadCrossing(
                chunk,
                JunctionForm.FOUR_WAY,
                0,
                1,
                EnumSet.allOf(ApproachDirection.class),
                512,
                256,
                4,
                demand,
                4,
                false,
                requireFreeFlow,
                new CrossingDecks(
                        new HalfBlockElevation(140),
                        new HalfBlockElevation(188),
                        new HalfBlockElevation(140),
                        new HalfBlockElevation(188)),
                seed);
        InterchangeDecision decision = InterchangeSelector.withBuiltIns()
                .select(crossing.selectionSite());
        return new PlannedInterchange(crossing, decision);
    }

    private static PlannedInterchange diamond(ChunkPoint chunk, CrossingDecks decks) {
        DetectedRoadCrossing crossing = new DetectedRoadCrossing(
                chunk,
                JunctionForm.FOUR_WAY,
                1,
                0,
                EnumSet.allOf(ApproachDirection.class),
                320,
                64,
                2,
                TrafficDemand.REGIONAL,
                2,
                false,
                false,
                decks,
                1L);
        InterchangeDecision decision = InterchangeSelector.withBuiltIns()
                .select(crossing.selectionSite());
        assertEquals(
                net.austizz.lostcitiesroadfixes.interchange.InterchangeType.DIAMOND,
                decision.selected().orElseThrow().type(),
                decision::diagnostic);
        return new PlannedInterchange(crossing, decision);
    }
}
