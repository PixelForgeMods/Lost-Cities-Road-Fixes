package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterchangeSelectorTest {
    private final InterchangeSelector selector = InterchangeSelector.withBuiltIns();

    @Test
    void catalogsEveryInterchangeShownInTheReferenceImage() {
        Set<InterchangeType> expected = EnumSet.of(
                InterchangeType.TRUMPET,
                InterchangeType.THREE_WAY_DIRECTIONAL,
                InterchangeType.SPUI,
                InterchangeType.PARTIAL_CLOVERLEAF,
                InterchangeType.SINGLE_QUADRANT,
                InterchangeType.DIAMOND,
                InterchangeType.CLOVERLEAF,
                InterchangeType.STACK);

        assertEquals(expected, InterchangeCatalogue.builtIns().stream()
                .map(InterchangeDesign::type)
                .collect(java.util.stream.Collectors.toSet()));
        assertEquals(8, InterchangeCatalogue.builtIns().size());
    }

    @Test
    void selectsEachFamilyForItsBestFitSite() {
        assertSelected(InterchangeType.TRUMPET,
                site(JunctionForm.THREE_WAY, 80, 2, TrafficDemand.REGIONAL, 2, true, true));
        assertSelected(InterchangeType.THREE_WAY_DIRECTIONAL,
                site(
                        JunctionForm.THREE_WAY,
                        128,
                        4,
                        TrafficDemand.HIGH,
                        3,
                        false,
                        true,
                        168));
        assertSelected(InterchangeType.SPUI,
                site(JunctionForm.FOUR_WAY, 48, 1, TrafficDemand.HIGH, 2, false, false));
        assertSelected(InterchangeType.PARTIAL_CLOVERLEAF,
                site(JunctionForm.FOUR_WAY, 96, 2, TrafficDemand.HIGH, 2, true, false));
        assertSelected(InterchangeType.SINGLE_QUADRANT,
                site(JunctionForm.FOUR_WAY, 64, 1, TrafficDemand.LOCAL, 2, false, false));
        assertSelected(InterchangeType.DIAMOND,
                site(JunctionForm.FOUR_WAY, 64, 2, TrafficDemand.REGIONAL, 2, false, false));
        assertSelected(InterchangeType.CLOVERLEAF,
                site(JunctionForm.FOUR_WAY, 256, 4, TrafficDemand.HIGH, 2, true, true));
        assertSelected(InterchangeType.STACK,
                site(
                        JunctionForm.FOUR_WAY,
                        128,
                        4,
                        TrafficDemand.HIGH,
                        4,
                        false,
                        true,
                        188));
    }

    @Test
    void lowGapRegionalCrossingSelectsDiamondAndRejectsFourLevelStack() {
        InterchangeSite site = site(
                JunctionForm.FOUR_WAY,
                128,
                4,
                TrafficDemand.REGIONAL,
                4,
                true,
                false);

        InterchangeDecision decision = selector.select(site);

        assertEquals(InterchangeType.DIAMOND,
                decision.selected().orElseThrow().type(), decision::diagnostic);
        assertEquals(192, decision.selectedApproachRunBlocks());
        InterchangeEvaluation stack = decision.evaluations().stream()
                .filter(evaluation -> evaluation.design().type() == InterchangeType.STACK)
                .findFirst()
                .orElseThrow();
        assertTrue(stack.rejectionReasons().stream()
                .anyMatch(reason -> reason.contains("four physical levels")),
                decision::diagnostic);
    }

    @Test
    void selectionDoesNotChangeWhenOnlyTheSeedChanges() {
        InterchangeSite first = site(
                JunctionForm.FOUR_WAY,
                128,
                4,
                TrafficDemand.HIGH,
                3,
                true,
                false,
                176,
                1L);
        InterchangeSite second = site(
                JunctionForm.FOUR_WAY,
                128,
                4,
                TrafficDemand.HIGH,
                3,
                true,
                false,
                176,
                Long.MAX_VALUE);

        assertEquals(
                selector.select(first).selected(),
                selector.select(second).selected());
    }

    @Test
    void rejectsSitesWithoutEnoughApproachForTheGrade() {
        InterchangeSite tooShort = new InterchangeSite(
                JunctionForm.FOUR_WAY,
                128,
                4,
                159,
                new HalfBlockElevation(140),
                new HalfBlockElevation(200),
                TrafficDemand.HIGH,
                4,
                true,
                false,
                99L);

        InterchangeDecision decision = selector.select(tooShort);

        assertTrue(decision.selected().isEmpty());
        assertTrue(decision.evaluations().stream()
                .flatMap(evaluation -> evaluation.rejectionReasons().stream())
                .anyMatch(reason -> reason.contains("turning-ramp grade requires 480")));
    }

    @Test
    void reportsAllRejectionsAndScoresInStableOrder() {
        InterchangeSite site = site(
                JunctionForm.FOUR_WAY, 64, 2, TrafficDemand.REGIONAL, 2, false, false);

        InterchangeDecision first = selector.select(site);
        InterchangeDecision second = selector.select(site);

        assertEquals(first, second);
        assertEquals(List.of(InterchangeType.values()), first.evaluations().stream()
                .map(evaluation -> evaluation.design().type())
                .toList());
        assertEquals(8, first.evaluations().size());
        assertTrue(first.evaluations().stream().allMatch(evaluation ->
                evaluation.feasible() == evaluation.score().isPresent()));
        assertFalse(first.evaluations().stream()
                .filter(evaluation -> !evaluation.feasible())
                .anyMatch(evaluation -> evaluation.rejectionReasons().isEmpty()));
    }

    private void assertSelected(InterchangeType expected, InterchangeSite site) {
        InterchangeDecision decision = selector.select(site);
        assertTrue(decision.selected().isPresent(), decision::diagnostic);
        assertEquals(expected, decision.selected().orElseThrow().type(), decision::diagnostic);
    }

    private static InterchangeSite site(
            JunctionForm form,
            int radius,
            int quadrants,
            TrafficDemand demand,
            int levels,
            boolean loopsAllowed,
            boolean requireFreeFlow) {
        return site(
                form,
                radius,
                quadrants,
                demand,
                levels,
                loopsAllowed,
                requireFreeFlow,
                160);
    }

    private static InterchangeSite site(
            JunctionForm form,
            int radius,
            int quadrants,
            TrafficDemand demand,
            int levels,
            boolean loopsAllowed,
            boolean requireFreeFlow,
            int upperHalfBlocks) {
        return site(
                form,
                radius,
                quadrants,
                demand,
                levels,
                loopsAllowed,
                requireFreeFlow,
                upperHalfBlocks,
                0x5eedL);
    }

    private static InterchangeSite site(
            JunctionForm form,
            int radius,
            int quadrants,
            TrafficDemand demand,
            int levels,
            boolean loopsAllowed,
            boolean requireFreeFlow,
            int upperHalfBlocks,
            long seed) {
        return new InterchangeSite(
                form,
                radius,
                quadrants,
                640,
                new HalfBlockElevation(140),
                new HalfBlockElevation(upperHalfBlocks),
                demand,
                levels,
                loopsAllowed,
                requireFreeFlow,
                seed);
    }
}
