package net.austizz.lostcitiesroadfixes.gametest;

import net.austizz.lostcitiesroadfixes.LostCitiesRoadFixes;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDecision;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeSelector;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.TrafficDemand;
import net.austizz.lostcitiesroadfixes.interchange.layout.ApproachDirection;
import net.austizz.lostcitiesroadfixes.interchange.planning.CrossingDecks;
import net.austizz.lostcitiesroadfixes.interchange.planning.DetectedRoadCrossing;
import net.austizz.lostcitiesroadfixes.interchange.planning.InterchangeConflictResolution;
import net.austizz.lostcitiesroadfixes.interchange.planning.InterchangeConflictResolver;
import net.austizz.lostcitiesroadfixes.interchange.planning.PlannedInterchange;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumSet;
import java.util.List;

@GameTestHolder(LostCitiesRoadFixes.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DenseCrossingGameTests {
    private DenseCrossingGameTests() {
    }

    @GameTest(template = "empty3x3x3", timeoutTicks = 20)
    public static void eightChunkInterchangeCoresResolveDeterministically(
            GameTestHelper helper) {
        InterchangeConflictResolver resolver =
                new InterchangeConflictResolver(RoadDesignStandard.DEFAULT);
        PlannedInterchange winner = candidate(new ChunkPoint(0, 0), 1L);
        PlannedInterchange blocked = candidate(new ChunkPoint(8, 0), 2L);

        InterchangeConflictResolution resolution = resolver.resolve(List.of(blocked, winner));
        if (!resolution.interchanges().equals(List.of(winner))
                || resolution.conflicts().size() != 1
                || !resolution.conflicts().getFirst().blockingCrossing()
                        .equals(winner.crossing().chunk())
                || resolver.surveyMarginChunks() != 34) {
            helper.fail("Dense interchange cores did not use the bounded stable winner");
            return;
        }
        helper.succeed();
    }

    private static PlannedInterchange candidate(ChunkPoint chunk, long seed) {
        DetectedRoadCrossing crossing = new DetectedRoadCrossing(
                chunk,
                JunctionForm.FOUR_WAY,
                0,
                1,
                EnumSet.allOf(ApproachDirection.class),
                512,
                256,
                4,
                TrafficDemand.HIGH,
                4,
                false,
                true,
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
