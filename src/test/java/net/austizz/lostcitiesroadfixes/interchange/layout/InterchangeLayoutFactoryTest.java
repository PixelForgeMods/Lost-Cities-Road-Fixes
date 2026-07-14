package net.austizz.lostcitiesroadfixes.interchange.layout;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeCatalogue;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesign;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesignId;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeType;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.TrafficDemand;
import net.austizz.lostcitiesroadfixes.interchange.geometry.PlanarPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterchangeLayoutFactoryTest {
    private static final double TOLERANCE = 1.0e-8;
    private static final InterchangeGeometrySite SITE = new InterchangeGeometrySite(
            new PlanarPoint(1_024.0, -2_048.0),
            200,
            new HalfBlockElevation(140),
            new HalfBlockElevation(160));

    private final InterchangeLayoutFactory factory =
            new InterchangeLayoutFactory(RoadDesignStandard.DEFAULT);

    @Test
    void instantiatesEveryRequestedFamilyWithEveryMovement() {
        for (InterchangeDesign design : InterchangeCatalogue.builtIns()) {
            InterchangeLayout layout = factory.create(design, SITE);
            int expectedMovements = design.form() == JunctionForm.THREE_WAY ? 6 : 12;
            Set<ApproachDirection> expectedApproaches = design.form() == JunctionForm.THREE_WAY
                    ? EnumSet.of(ApproachDirection.WEST, ApproachDirection.EAST, ApproachDirection.SOUTH)
                    : EnumSet.allOf(ApproachDirection.class);

            assertEquals(design, layout.design());
            assertEquals(expectedApproaches, layout.approaches());
            assertEquals(expectedMovements, layout.connections().size(), design.id().toString());
            assertEquals(expectedMovements, layout.connections().stream()
                    .map(InterchangeConnection::movement)
                    .collect(java.util.stream.Collectors.toSet())
                    .size(), "duplicate movement in " + design.id());
            assertEquals(design.freeFlowMovementCount(), layout.connections().stream()
                    .filter(connection -> connection.control() == RampControl.FREE_FLOW)
                    .count(), design.id().toString());
            assertEquals(design.structureLevels(), layout.connections().stream()
                    .mapToInt(InterchangeConnection::structureLevel)
                    .max()
                    .orElseThrow(), design.id().toString());
        }
    }

    @Test
    void usesTheExpectedLoopCountForEachFamily() {
        Map<InterchangeType, Integer> expected = new EnumMap<>(InterchangeType.class);
        expected.put(InterchangeType.TRUMPET, 1);
        expected.put(InterchangeType.THREE_WAY_DIRECTIONAL, 0);
        expected.put(InterchangeType.SPUI, 0);
        expected.put(InterchangeType.PARTIAL_CLOVERLEAF, 2);
        expected.put(InterchangeType.SINGLE_QUADRANT, 0);
        expected.put(InterchangeType.DIAMOND, 0);
        expected.put(InterchangeType.CLOVERLEAF, 4);
        expected.put(InterchangeType.STACK, 0);

        for (InterchangeDesign design : InterchangeCatalogue.builtIns()) {
            InterchangeLayout layout = factory.create(design, SITE);
            assertEquals(expected.get(design.type()).longValue(), layout.connections().stream()
                    .filter(connection -> connection.form() == RampForm.LOOP)
                    .count(), design.id().toString());
        }
    }

    @Test
    void everyConnectionUsesTheCorrectPortsAndDeckElevations() {
        for (InterchangeDesign design : InterchangeCatalogue.builtIns()) {
            InterchangeLayout layout = factory.create(design, SITE);
            for (InterchangeConnection connection : layout.connections()) {
                InterchangePort start = SITE.port(
                        connection.movement().from(), TrafficFlow.INBOUND);
                InterchangePort end = SITE.port(
                        connection.movement().to(), TrafficFlow.OUTBOUND);

                assertPoint(start.point(), connection.route().centerline().startPose().point());
                assertPoint(end.point(), connection.route().centerline().endPose().point());
                assertEquals(start.elevation(), connection.route().centerline().startElevation());
                assertEquals(end.elevation(), connection.route().centerline().endElevation());
                assertEquals(8, connection.route().widthBlocks());
            }
        }
    }

    @Test
    void routesStayInsideTheApproachEnvelopeAndAreDeterministic() {
        for (InterchangeDesign design : InterchangeCatalogue.builtIns()) {
            InterchangeLayout first = factory.create(design, SITE);
            InterchangeLayout second = factory.create(design, SITE);

            assertEquals(first, second);
            first.connections().forEach(connection -> connection.route().centerline().samples()
                    .forEach(sample -> {
                        assertTrue(StrictMath.abs(sample.point().x() - SITE.center().x())
                                <= SITE.approachRunBlocks() + TOLERANCE, design.id().toString());
                        assertTrue(StrictMath.abs(sample.point().z() - SITE.center().z())
                                <= SITE.approachRunBlocks() + TOLERANCE, design.id().toString());
                    }));
        }
    }

    @Test
    void controlledFamiliesContainConflictsWhileFreeFlowFamiliesDoNot() {
        for (InterchangeDesign design : InterchangeCatalogue.builtIns()) {
            InterchangeLayout layout = factory.create(design, SITE);
            boolean hasControlledConnection = layout.connections().stream()
                    .anyMatch(connection -> connection.control() != RampControl.FREE_FLOW);
            if (design.allMovementsFreeFlow()) {
                assertFalse(hasControlledConnection, design.id().toString());
            } else {
                assertTrue(hasControlledConnection, design.id().toString());
            }
        }
    }

    @Test
    void compilesCustomMovementFormsAndPropertiesToExactNativePorts() {
        InterchangeDesign design = customFourWayDesign();

        InterchangeLayout layout = factory.create(design, SITE);

        InterchangeConnection direct = connection(
                layout, ApproachDirection.WEST, ApproachDirection.SOUTH);
        assertEquals(RampForm.DIRECT, direct.form());
        assertEquals(RampControl.YIELD, direct.control());
        assertEquals(11, direct.route().widthBlocks());
        assertEquals(1, direct.structureLevel());

        InterchangeConnection loop = connection(
                layout, ApproachDirection.WEST, ApproachDirection.NORTH);
        assertEquals(RampForm.LOOP, loop.form());
        assertEquals(RampControl.FREE_FLOW, loop.control());
        assertEquals(10, loop.route().widthBlocks());
        assertEquals(2, loop.structureLevel());

        assertExactPorts(layout);
    }

    @Test
    void rotatesCanonicalThreeWayGeometryToTheSurveyedMissingApproach() {
        InterchangeDesign design = customThreeWayDesign();
        Set<ApproachDirection> approaches = EnumSet.of(
                ApproachDirection.NORTH,
                ApproachDirection.SOUTH,
                ApproachDirection.WEST);

        InterchangeLayout layout = factory.create(design, SITE, approaches);

        assertEquals(approaches, layout.approaches());
        assertEquals(6, layout.connections().size());
        assertEquals(13, connection(
                layout, ApproachDirection.NORTH, ApproachDirection.WEST)
                .route().widthBlocks(),
                "canonical west-to-south must rotate when east is the missing approach");
        assertExactPorts(layout);
    }

    private static InterchangeConnection connection(
            InterchangeLayout layout,
            ApproachDirection from,
            ApproachDirection to) {
        return layout.connections().stream()
                .filter(candidate -> candidate.movement().from() == from
                        && candidate.movement().to() == to)
                .findFirst()
                .orElseThrow();
    }

    private static void assertExactPorts(InterchangeLayout layout) {
        for (InterchangeConnection connection : layout.connections()) {
            InterchangePort start = layout.site().port(
                    connection.movement().from(), TrafficFlow.INBOUND);
            InterchangePort end = layout.site().port(
                    connection.movement().to(), TrafficFlow.OUTBOUND);
            assertPoint(start.point(), connection.route().centerline().startPose().point());
            assertPoint(end.point(), connection.route().centerline().endPose().point());
            assertEquals(start.elevation(), connection.route().centerline().startElevation());
            assertEquals(end.elevation(), connection.route().centerline().endElevation());
        }
    }

    private static InterchangeDesign customFourWayDesign() {
        List<InterchangeMovementBlueprint> movements = new ArrayList<>();
        for (ApproachDirection from : ApproachDirection.values()) {
            movements.add(blueprint(from, MovementKind.STRAIGHT, RampForm.MAINLINE,
                    RampControl.FREE_FLOW, 8, 1));
            movements.add(blueprint(from, MovementKind.RIGHT, RampForm.DIRECT,
                    RampControl.YIELD, from == ApproachDirection.WEST ? 11 : 8, 1));
            boolean loop = from == ApproachDirection.WEST;
            movements.add(blueprint(from, MovementKind.LEFT,
                    loop ? RampForm.LOOP : RampForm.DIRECT,
                    loop ? RampControl.FREE_FLOW : RampControl.SIGNALIZED,
                    loop ? 10 : 8,
                    2));
        }
        return design(
                "example:custom_four_way",
                InterchangeType.PARTIAL_CLOVERLEAF,
                JunctionForm.FOUR_WAY,
                2,
                true,
                false,
                5,
                movements);
    }

    private static InterchangeDesign customThreeWayDesign() {
        Set<ApproachDirection> canonical = EnumSet.of(
                ApproachDirection.WEST,
                ApproachDirection.EAST,
                ApproachDirection.SOUTH);
        List<InterchangeMovementBlueprint> movements = new ArrayList<>();
        for (ApproachDirection from : canonical) {
            for (MovementKind kind : MovementKind.values()) {
                ApproachDirection to = destination(from, kind);
                if (!canonical.contains(to)) {
                    continue;
                }
                movements.add(blueprint(
                        from,
                        kind,
                        kind == MovementKind.STRAIGHT ? RampForm.MAINLINE : RampForm.DIRECT,
                        RampControl.FREE_FLOW,
                        from == ApproachDirection.WEST && kind == MovementKind.RIGHT ? 13 : 8,
                        kind == MovementKind.LEFT ? 2 : 1));
            }
        }
        return design(
                "example:custom_three_way",
                InterchangeType.THREE_WAY_DIRECTIONAL,
                JunctionForm.THREE_WAY,
                2,
                false,
                true,
                6,
                movements);
    }

    private static InterchangeDesign design(
            String id,
            InterchangeType type,
            JunctionForm form,
            int levels,
            boolean loops,
            boolean allFreeFlow,
            int freeFlowMovements,
            List<InterchangeMovementBlueprint> movements) {
        return new InterchangeDesign(
                InterchangeDesignId.parse(id),
                type,
                form,
                64,
                2,
                112,
                levels,
                loops,
                allFreeFlow,
                TrafficDemand.REGIONAL,
                freeFlowMovements,
                3,
                Optional.of(new InterchangeGeometryBlueprint(movements)));
    }

    private static InterchangeMovementBlueprint blueprint(
            ApproachDirection from,
            MovementKind kind,
            RampForm form,
            RampControl control,
            int width,
            int level) {
        return new InterchangeMovementBlueprint(
                new InterchangeMovement(from, destination(from, kind), kind),
                form,
                control,
                width,
                level);
    }

    private static ApproachDirection destination(
            ApproachDirection from,
            MovementKind kind) {
        return switch (kind) {
            case STRAIGHT -> from.opposite();
            case RIGHT -> from.rightTurnDestination();
            case LEFT -> from.leftTurnDestination();
        };
    }

    private static void assertPoint(PlanarPoint expected, PlanarPoint actual) {
        assertEquals(expected.x(), actual.x(), TOLERANCE);
        assertEquals(expected.z(), actual.z(), TOLERANCE);
    }
}
