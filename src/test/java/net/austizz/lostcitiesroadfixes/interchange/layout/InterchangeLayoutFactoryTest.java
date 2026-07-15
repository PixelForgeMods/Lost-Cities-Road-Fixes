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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
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
            640,
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
    void everyBuiltInCompilesAtItsAdvertisedMinimumApproach() {
        List<String> understatedMinimums = new ArrayList<>();
        for (InterchangeDesign design : InterchangeCatalogue.builtIns()) {
            int firstCompilable = -1;
            for (int approach = design.minimumApproachRunBlocks(); approach <= 640;
                 approach += 16) {
                InterchangeGeometrySite site = new InterchangeGeometrySite(
                        new PlanarPoint(0.0, 0.0),
                        approach,
                        new HalfBlockElevation(140),
                        new HalfBlockElevation(160));
                try {
                    factory.create(design, site);
                    firstCompilable = approach;
                    break;
                } catch (IllegalArgumentException ignored) {
                    // Keep searching chunk-aligned approach lengths for the safe minimum.
                }
            }
            if (firstCompilable != design.minimumApproachRunBlocks()) {
                understatedMinimums.add(
                        design.id() + " declares " + design.minimumApproachRunBlocks()
                                + " but first compiles at " + firstCompilable);
            }
        }
        assertTrue(understatedMinimums.isEmpty(), understatedMinimums.toString());
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
    void fullCloverleafKeepsEveryEntranceAndExitOnDistinctTerminals() {
        InterchangeDesign cloverleaf = InterchangeCatalogue.builtIns().stream()
                .filter(design -> design.type() == InterchangeType.CLOVERLEAF)
                .findFirst()
                .orElseThrow();
        InterchangeLayout layout = factory.create(cloverleaf, SITE);

        for (ApproachDirection approach : ApproachDirection.values()) {
            List<InterchangeConnection> exits = layout.connections().stream()
                    .filter(connection -> connection.movement().from() == approach)
                    .filter(connection -> connection.form() != RampForm.MAINLINE)
                    .toList();
            List<InterchangeConnection> entrances = layout.connections().stream()
                    .filter(connection -> connection.movement().to() == approach)
                    .filter(connection -> connection.form() != RampForm.MAINLINE)
                    .toList();

            assertEquals(2, exits.size());
            assertEquals(2, entrances.size());
            assertTrue(distance(
                    exits.get(0).route().centerline().startPose().point(),
                    exits.get(1).route().centerline().startPose().point()) >= 16.0,
                    () -> approach + " exit ramps share one terminal");
            assertTrue(distance(
                    entrances.get(0).route().centerline().endPose().point(),
                    entrances.get(1).route().centerline().endPose().point()) >= 16.0,
                    () -> approach + " entrance ramps share one terminal");
        }
    }

    @Test
    void fullCloverleafTurningMovementsDoNotShareAnUndeclaredSingleLane() {
        InterchangeDesign cloverleaf = InterchangeCatalogue.builtIns().stream()
                .filter(design -> design.type() == InterchangeType.CLOVERLEAF)
                .findFirst()
                .orElseThrow();
        List<InterchangeConnection> turns = factory.create(cloverleaf, SITE)
                .connections().stream()
                .filter(connection -> connection.form() != RampForm.MAINLINE)
                .toList();

        for (int left = 0; left < turns.size(); left++) {
            Set<String> leftInterior = interiorSamples(turns.get(left));
            for (int right = left + 1; right < turns.size(); right++) {
                Set<String> shared = new HashSet<>(leftInterior);
                shared.retainAll(interiorSamples(turns.get(right)));
                assertTrue(shared.isEmpty(),
                        turns.get(left).movement() + " and " + turns.get(right).movement()
                                + " share an undeclared lane segment");
            }
        }
    }

    @Test
    void fullCloverleafHasNoInteriorAtGradeRampCrossings() {
        InterchangeDesign cloverleaf = InterchangeCatalogue.builtIns().stream()
                .filter(design -> design.type() == InterchangeType.CLOVERLEAF)
                .findFirst()
                .orElseThrow();
        InterchangeLayout layout = factory.create(cloverleaf, SITE);
        List<InterchangeConnection> turns = layout.connections().stream()
                .filter(connection -> connection.form() != RampForm.MAINLINE)
                .toList();

        for (int left = 0; left < turns.size(); left++) {
            for (int right = left + 1; right < turns.size(); right++) {
                assertNoInteriorAtGradeApproach(
                        turns.get(left), turns.get(right), layout.auxiliaryLanes());
            }
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

                if (connection.form() == RampForm.MAINLINE) {
                    assertPoint(start.point(), connection.route().centerline().startPose().point());
                    assertPoint(end.point(), connection.route().centerline().endPose().point());
                } else {
                    assertTrue(distance(SITE.center(),
                            connection.route().centerline().startPose().point())
                            < SITE.approachRunBlocks());
                    assertTrue(distance(SITE.center(),
                            connection.route().centerline().endPose().point())
                            < SITE.approachRunBlocks());
                }
                assertEquals(start.elevation(), connection.route().centerline().startElevation());
                assertEquals(end.elevation(), connection.route().centerline().endElevation());
                assertEquals(8, connection.route().widthBlocks());
            }
        }
    }

    @Test
    void builtInsAddOneContinuousAuxiliaryLanePerMainlineMovement() {
        for (InterchangeDesign design : InterchangeCatalogue.builtIns()) {
            InterchangeLayout layout = factory.create(design, SITE);
            long mainlines = layout.connections().stream()
                    .filter(connection -> connection.form() == RampForm.MAINLINE)
                    .count();

            assertEquals(mainlines, layout.auxiliaryLanes().size(), design.id().toString());
            for (InterchangeAuxiliaryLane lane : layout.auxiliaryLanes()) {
                assertPoint(
                        SITE.outerThroughPort(
                                lane.mainlineMovement().from(), TrafficFlow.INBOUND).point(),
                        lane.route().centerline().startPose().point());
                assertPoint(
                        SITE.outerThroughPort(
                                lane.mainlineMovement().to(), TrafficFlow.OUTBOUND).point(),
                        lane.route().centerline().endPose().point());
                double maximumCrossOffset = lane.route().centerline().samples().stream()
                        .mapToDouble(sample -> crossOffset(
                                lane.mainlineMovement().from(), sample.point()))
                        .max()
                        .orElseThrow();
                assertEquals(20.0, maximumCrossOffset, 0.01, design.id().toString());
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

    private static Set<String> interiorSamples(InterchangeConnection connection) {
        double length = connection.route().centerline().lengthBlocks();
        return connection.route().centerline().samples().stream()
                .filter(sample -> sample.stationBlocks() >= 8.0)
                .filter(sample -> sample.stationBlocks() <= length - 8.0)
                .map(sample -> Math.round(sample.point().x() * 1_000.0) + ":"
                        + Math.round(sample.point().z() * 1_000.0) + ":"
                        + sample.elevation().halfBlocks())
                .collect(java.util.stream.Collectors.toSet());
    }

    private static void assertNoInteriorAtGradeApproach(
            InterchangeConnection left,
            InterchangeConnection right,
            List<InterchangeAuxiliaryLane> auxiliaryLanes) {
        double leftLength = left.route().centerline().lengthBlocks();
        double rightLength = right.route().centerline().lengthBlocks();
        for (var leftSample : left.route().centerline().samples()) {
            if (leftSample.stationBlocks() < 16.0
                    || leftSample.stationBlocks() > leftLength - 16.0) {
                continue;
            }
            for (var rightSample : right.route().centerline().samples()) {
                if (rightSample.stationBlocks() < 16.0
                        || rightSample.stationBlocks() > rightLength - 16.0) {
                    continue;
                }
                if (distance(leftSample.point(), rightSample.point()) >= 2.0) {
                    continue;
                }
                double clearance = StrictMath.abs(
                        leftSample.elevation().blocks() - rightSample.elevation().blocks());
                if (clearance < 7.0 && isDeclaredAuxiliaryOverlap(
                        left, right, leftSample.point(), auxiliaryLanes)) {
                    continue;
                }
                assertTrue(clearance >= 7.0,
                        left.movement() + " crosses " + right.movement()
                                + " at grade near " + leftSample.point()
                                + " (leftY=" + leftSample.elevation().blocks()
                                + ", rightY=" + rightSample.elevation().blocks() + ')');
            }
        }
    }

    private static boolean isDeclaredAuxiliaryOverlap(
            InterchangeConnection left,
            InterchangeConnection right,
            PlanarPoint point,
            List<InterchangeAuxiliaryLane> auxiliaryLanes) {
        return auxiliaryLanes.stream()
                .filter(lane -> touchesAuxiliary(left, lane)
                        && touchesAuxiliary(right, lane))
                .flatMap(lane -> lane.route().centerline().samples().stream())
                .anyMatch(sample -> distance(sample.point(), point) < 12.5);
    }

    private static boolean touchesAuxiliary(
            InterchangeConnection turn,
            InterchangeAuxiliaryLane lane) {
        return turn.movement().from() == lane.mainlineMovement().from()
                || turn.movement().to() == lane.mainlineMovement().to();
    }

    private static double distance(PlanarPoint left, PlanarPoint right) {
        return StrictMath.hypot(left.x() - right.x(), left.z() - right.z());
    }

    private static double crossOffset(
            ApproachDirection direction,
            PlanarPoint point) {
        return switch (direction) {
            case EAST, WEST -> StrictMath.abs(point.z() - SITE.center().z());
            case NORTH, SOUTH -> StrictMath.abs(point.x() - SITE.center().x());
        };
    }

    private static void assertPoint(PlanarPoint expected, PlanarPoint actual) {
        assertEquals(expected.x(), actual.x(), TOLERANCE);
        assertEquals(expected.z(), actual.z(), TOLERANCE);
    }
}
