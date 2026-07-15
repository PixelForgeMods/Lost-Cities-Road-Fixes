package net.austizz.lostcitiesroadfixes.diagnostics;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDecision;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeSelector;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.TrafficDemand;
import net.austizz.lostcitiesroadfixes.interchange.layout.ApproachDirection;
import net.austizz.lostcitiesroadfixes.interchange.planning.CrossingDecks;
import net.austizz.lostcitiesroadfixes.interchange.planning.DetectedRoadCrossing;
import net.austizz.lostcitiesroadfixes.interchange.planning.PlannedInterchange;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterchangeExplanationTest {
    @Test
    void rendersStableImmutableLinesForAChunkWithoutACrossing() {
        InterchangeExplanation explanation = InterchangeExplanation.none(
                new ChunkPoint(-12, 34));

        assertEquals(List.of(
                "Interchange explanation for chunk -12,34",
                "outcome: no_crossing",
                "No differing-height Lost Cities road crossing was detected in this chunk"),
                explanation.lines());
        assertThrows(
                UnsupportedOperationException.class,
                () -> explanation.lines().clear());
    }

    @Test
    void rejectsBlankDetails() {
        assertThrows(IllegalArgumentException.class, () -> new InterchangeExplanation(
                new ChunkPoint(0, 0),
                InterchangeExplanation.Outcome.REJECTED,
                List.of(" ")));
    }

    @Test
    void selectedExplanationIncludesMeasuredAndCompiledFacts() {
        DetectedRoadCrossing crossing = new DetectedRoadCrossing(
                new ChunkPoint(1, 2),
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
                        new HalfBlockElevation(152),
                        new HalfBlockElevation(164),
                        new HalfBlockElevation(152),
                        new HalfBlockElevation(172)),
                99L);
        InterchangeDecision decision = InterchangeSelector.withBuiltIns()
                .select(crossing.selectionSite());

        List<String> lines = InterchangeExplanation.selected(
                new PlannedInterchange(crossing, decision)).lines();

        assertEquals("outcome: selected", lines.get(1));
        assertTrue(lines.stream()
                .anyMatch(line -> line.contains("family=diamond")));
        assertTrue(lines.stream()
                .anyMatch(line -> line.contains("plannedGapBlocks=10.0")));
        assertTrue(lines.stream()
                .anyMatch(line -> line.contains("compiledApproachBlocks=192")));
        assertTrue(lines.stream()
                .anyMatch(line -> line.contains("displacedBuildings=0")));
    }
}
