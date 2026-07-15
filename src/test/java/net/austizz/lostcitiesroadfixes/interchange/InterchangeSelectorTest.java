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
                site(JunctionForm.THREE_WAY, 128, 4, TrafficDemand.HIGH, 3, false, true));
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
                site(JunctionForm.FOUR_WAY, 128, 4, TrafficDemand.HIGH, 4, false, true));
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
        return new InterchangeSite(
                form,
                radius,
                quadrants,
                640,
                new HalfBlockElevation(140),
                new HalfBlockElevation(160),
                demand,
                levels,
                loopsAllowed,
                requireFreeFlow,
                0x5eedL);
    }
}
