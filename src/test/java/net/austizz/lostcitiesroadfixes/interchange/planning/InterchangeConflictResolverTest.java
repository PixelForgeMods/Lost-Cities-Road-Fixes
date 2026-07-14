package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDecision;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeSelector;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.TrafficDemand;
import net.austizz.lostcitiesroadfixes.interchange.layout.ApproachDirection;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals(256, conflict.minimumCenterSeparationBlocks());
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
    void higherDemandWinsBeforeTheUnsignedSeedTieBreaker() {
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
        PlannedInterchange middle = candidate(new ChunkPoint(8, 0), 3L);
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
    void surveyHaloCoversTheLargestSelectableCorePair() {
        assertEquals(18, RESOLVER.surveyMarginChunks());
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
                256,
                128,
                4,
                demand,
                4,
                true,
                requireFreeFlow,
                new CrossingDecks(
                        new HalfBlockElevation(140),
                        new HalfBlockElevation(152),
                        new HalfBlockElevation(140),
                        new HalfBlockElevation(160)),
                seed);
        InterchangeDecision decision = InterchangeSelector.withBuiltIns()
                .select(crossing.selectionSite());
        return new PlannedInterchange(crossing, decision);
    }
}
