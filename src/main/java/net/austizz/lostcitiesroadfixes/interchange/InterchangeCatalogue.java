package net.austizz.lostcitiesroadfixes.interchange;

import java.util.List;

public final class InterchangeCatalogue {
    private static final List<InterchangeDesign> BUILT_INS = List.of(
            design(InterchangeType.TRUMPET, JunctionForm.THREE_WAY,
                    64, 2, 96, 2, true, true, TrafficDemand.REGIONAL, 6, 3),
            design(InterchangeType.THREE_WAY_DIRECTIONAL, JunctionForm.THREE_WAY,
                    96, 3, 128, 3, false, true, TrafficDemand.HIGH, 6, 5),
            design(InterchangeType.SPUI, JunctionForm.FOUR_WAY,
                    40, 1, 96, 2, false, false, TrafficDemand.HIGH, 2, 3),
            design(InterchangeType.PARTIAL_CLOVERLEAF, JunctionForm.FOUR_WAY,
                    80, 2, 128, 2, true, false, TrafficDemand.HIGH, 4, 2),
            design(InterchangeType.SINGLE_QUADRANT, JunctionForm.FOUR_WAY,
                    56, 1, 96, 2, false, false, TrafficDemand.LOCAL, 2, 1),
            design(InterchangeType.DIAMOND, JunctionForm.FOUR_WAY,
                    56, 2, 96, 2, false, false, TrafficDemand.REGIONAL, 2, 1),
            design(InterchangeType.CLOVERLEAF, JunctionForm.FOUR_WAY,
                    112, 4, 160, 2, true, true, TrafficDemand.HIGH, 8, 5),
            design(InterchangeType.STACK, JunctionForm.FOUR_WAY,
                    96, 4, 160, 4, false, true, TrafficDemand.HIGH, 8, 8));

    private InterchangeCatalogue() {
    }

    public static List<InterchangeDesign> builtIns() {
        return BUILT_INS;
    }

    private static InterchangeDesign design(
            InterchangeType type,
            JunctionForm form,
            int radius,
            int quadrants,
            int approach,
            int levels,
            boolean loops,
            boolean freeFlow,
            TrafficDemand capacity,
            int freeFlowMovements,
            int complexity) {
        return new InterchangeDesign(
                type,
                form,
                radius,
                quadrants,
                approach,
                levels,
                loops,
                freeFlow,
                capacity,
                freeFlowMovements,
                complexity);
    }
}
