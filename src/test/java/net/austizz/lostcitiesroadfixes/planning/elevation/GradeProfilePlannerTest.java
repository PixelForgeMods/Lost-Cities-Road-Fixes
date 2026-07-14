package net.austizz.lostcitiesroadfixes.planning.elevation;

import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradeProfilePlannerTest {
    private final GradeProfilePlanner planner = new GradeProfilePlanner(RoadDesignStandard.DEFAULT);

    @Test
    void plansAnExactSixBlockLostCitiesLevelTransition() {
        HalfBlockElevation start = HalfBlockElevation.ofWholeBlocks(71);
        HalfBlockElevation end = HalfBlockElevation.ofWholeBlocks(77);

        GradePlanResult result = planner.plan(start, end, 96);
        ElevationProfile profile = result.requireProfile();

        assertTrue(result.feasible());
        assertEquals(96, result.minimumRequiredRunBlocks());
        assertEquals(97, profile.samples().size());
        assertEquals(start, profile.elevationAt(0));
        assertEquals(end, profile.elevationAt(96));
        assertEquals(List.of(8, 16, 24, 32, 40, 48, 56, 64, 72, 80, 88, 96),
                profile.halfStepPositions());
        assertObeysOneHalfStepPerEightBlocks(profile);
    }

    @Test
    void rejectsAOneBlockTooShortTransitionWithRequiredRun() {
        GradePlanResult result = planner.plan(
                HalfBlockElevation.ofWholeBlocks(71),
                HalfBlockElevation.ofWholeBlocks(77),
                95);

        assertFalse(result.feasible());
        assertEquals(96, result.minimumRequiredRunBlocks());
        assertTrue(result.diagnostic().contains("96"));
        assertThrows(IllegalStateException.class, result::requireProfile);
    }

    @Test
    void requiresOneHundredSixtyBlocksForPreferredDeckSeparation() {
        HalfBlockElevation lower = HalfBlockElevation.ofWholeBlocks(70);
        HalfBlockElevation upper = HalfBlockElevation.ofWholeBlocks(80);

        assertEquals(160, planner.minimumRunBlocks(lower, upper));
        assertFalse(planner.plan(lower, upper, 159).feasible());
        assertTrue(planner.plan(lower, upper, 160).feasible());
    }

    @Test
    void descendingProfileMirrorsAscendingProfileDeterministically() {
        HalfBlockElevation low = new HalfBlockElevation(140);
        HalfBlockElevation high = new HalfBlockElevation(145);
        ElevationProfile ascending = planner.plan(low, high, 43).requireProfile();
        ElevationProfile descending = planner.plan(high, low, 43).requireProfile();

        for (int distance = 0; distance <= 43; distance++) {
            assertEquals(
                    ascending.elevationAt(43 - distance),
                    descending.elevationAt(distance));
        }
        assertEquals(ascending, planner.plan(low, high, 43).requireProfile());
        assertObeysOneHalfStepPerEightBlocks(ascending);
        assertObeysOneHalfStepPerEightBlocks(descending);
    }

    @Test
    void flatProfileKeepsEverySampleAtTheRequestedElevation() {
        HalfBlockElevation elevation = new HalfBlockElevation(143);
        ElevationProfile profile = planner.plan(elevation, elevation, 32).requireProfile();

        assertEquals(0, planner.minimumRunBlocks(elevation, elevation));
        assertEquals(33, profile.samples().size());
        assertTrue(profile.samples().stream().allMatch(elevation::equals));
        assertTrue(profile.halfStepPositions().isEmpty());
    }

    private static void assertObeysOneHalfStepPerEightBlocks(ElevationProfile profile) {
        Integer previousStep = null;
        for (int step : profile.halfStepPositions()) {
            if (previousStep != null) {
                assertTrue(step - previousStep >= 8,
                        "Half steps were only " + (step - previousStep) + " blocks apart");
            }
            previousStep = step;
        }
    }
}
