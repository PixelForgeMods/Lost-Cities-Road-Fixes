package net.austizz.lostcitiesroadfixes.interchange.layout;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeCatalogue;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesign;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesignId;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeType;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.TrafficDemand;
import net.austizz.lostcitiesroadfixes.interchange.geometry.PlanarPoint;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampRoute;
import net.austizz.lostcitiesroadfixes.interchange.render.GradedArterial;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterchangeLayoutFactoryTest {
    private static final double TOLERANCE = 1.0e-8;
    private static final InterchangeGeometrySite SITE = new InterchangeGeometrySite(
            new PlanarPoint(1_024.0, -2_048.0),
            640,
            new HalfBlockElevation(140),
            new HalfBlockElevation(160));
    private static final InterchangeGeometrySite GRADED_SITE = new InterchangeGeometrySite(
            new PlanarPoint(1_024.0, -2_048.0),
            640,
            new HalfBlockElevation(140),
            new HalfBlockElevation(152),
            new HalfBlockElevation(140),
            new HalfBlockElevation(160));
    private static final InterchangeGeometrySite STACK_SITE = new InterchangeGeometrySite(
            new PlanarPoint(1_024.0, -2_048.0),
            640,
            new HalfBlockElevation(140),
            new HalfBlockElevation(188));
    private static final InterchangeGeometrySite CUSTOM_SITE = new InterchangeGeometrySite(
            new PlanarPoint(1_024.0, -2_048.0),
            640,
            new HalfBlockElevation(140),
            new HalfBlockElevation(154));

    private final InterchangeLayoutFactory factory =
            new InterchangeLayoutFactory(RoadDesignStandard.DEFAULT);

    @Test
    void instantiatesEveryRequestedFamilyWithEveryMovement() {
        for (InterchangeDesign design : InterchangeCatalogue.builtIns()) {
            InterchangeLayout layout = factory.create(design, siteFor(design));
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
                        new HalfBlockElevation(design.type() == InterchangeType.STACK
                                ? 188 : 160));
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
            InterchangeLayout layout = factory.create(design, siteFor(design));
            assertEquals(expected.get(design.type()).longValue(), layout.connections().stream()
                    .filter(connection -> connection.form() == RampForm.LOOP)
                    .count(), design.id().toString());
        }
    }

    @Test
    void fullCloverleafKeepsOuterRampsAndLocalLoopsOnSeparateCollectorStations() {
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
                    exits.get(1).route().centerline().startPose().point()) >= 32.0,
                    () -> approach + " outer and loop exits share one station");
            assertTrue(distance(
                    entrances.get(0).route().centerline().endPose().point(),
                    entrances.get(1).route().centerline().endPose().point()) >= 32.0,
                    () -> approach + " outer and loop entrances share one station");
        }
    }

    @Test
    void fullCloverleafSharedTrunksSeparateIntoDistinctInteriorRoutes() {
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
                InterchangeConnection leftTurn = turns.get(left);
                InterchangeConnection rightTurn = turns.get(right);
                if (leftTurn.movement().from() == rightTurn.movement().from()
                        || leftTurn.movement().to() == rightTurn.movement().to()) {
                    continue;
                }
                Set<String> shared = new HashSet<>(leftInterior);
                shared.retainAll(interiorSamples(rightTurn));
                assertTrue(shared.isEmpty(),
                        leftTurn.movement() + " and " + rightTurn.movement()
                                + " share an unrelated interior lane segment");
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
            InterchangeGeometrySite site = siteFor(design);
            InterchangeLayout layout = factory.create(design, site);
            for (InterchangeConnection connection : layout.connections()) {
                InterchangePort start = site.port(
                        connection.movement().from(), TrafficFlow.INBOUND);
                InterchangePort end = site.port(
                        connection.movement().to(), TrafficFlow.OUTBOUND);

                if (connection.form() == RampForm.MAINLINE) {
                    assertPoint(start.point(), connection.route().centerline().startPose().point());
                    assertPoint(end.point(), connection.route().centerline().endPose().point());
                } else {
                    assertTrue(distance(site.center(),
                            connection.route().centerline().startPose().point())
                            < site.approachRunBlocks());
                    assertTrue(distance(site.center(),
                            connection.route().centerline().endPose().point())
                            < site.approachRunBlocks());
                }
                assertEquals(start.elevation(), connection.route().centerline().startElevation());
                assertEquals(end.elevation(), connection.route().centerline().endElevation());
                assertEquals(10, connection.route().widthBlocks());
            }
        }
    }

    @Test
    void stackTurningRoutesUseOneMonotonicGradeBetweenTheirTerminals() {
        InterchangeDesign stack = InterchangeCatalogue.builtIns().stream()
                .filter(design -> design.type() == InterchangeType.STACK)
                .findFirst()
                .orElseThrow();

        List<InterchangeConnection> turns = factory.create(stack, STACK_SITE).connections().stream()
                .filter(connection -> connection.form() != RampForm.MAINLINE)
                .toList();

        for (InterchangeConnection turn : turns) {
            int start = turn.route().centerline().startElevation().halfBlocks();
            int end = turn.route().centerline().endElevation().halfBlocks();
            int minimum = StrictMath.min(start, end);
            int maximum = StrictMath.max(start, end);
            int previous = start;
            for (var sample : turn.route().centerline().samples()) {
                int current = sample.elevation().halfBlocks();
                assertTrue(current >= minimum && current <= maximum,
                        () -> turn.movement() + " leaves its terminal elevation range at "
                                + sample.stationBlocks() + ": " + current);
                if (end >= start) {
                    assertTrue(current >= previous,
                            () -> turn.movement() + " dips at " + sample.stationBlocks());
                } else {
                    assertTrue(current <= previous,
                            () -> turn.movement() + " rises during a descending ramp at "
                                    + sample.stationBlocks());
                }
                previous = current;
            }
        }
    }

    @Test
    void stackRequiresFourPhysicalLevelsBetweenItsMainlineDecks() {
        InterchangeDesign stack = InterchangeCatalogue.builtIns().stream()
                .filter(design -> design.type() == InterchangeType.STACK)
                .findFirst()
                .orElseThrow();

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> factory.create(stack, GRADED_SITE));

        assertTrue(error.getMessage().contains("four physical levels"), error::getMessage);
    }

    @Test
    void auxiliaryRampPortsUseTheLocalGradedApproachElevation() {
        InterchangeGeometrySite graded = new InterchangeGeometrySite(
                new PlanarPoint(0.0, 0.0),
                640,
                new HalfBlockElevation(140),
                new HalfBlockElevation(160),
                new HalfBlockElevation(164),
                new HalfBlockElevation(176));

        assertEquals(
                new HalfBlockElevation(152),
                graded.auxiliaryPort(
                        ApproachDirection.WEST,
                        TrafficFlow.INBOUND,
                        544).elevation());
        assertEquals(
                new HalfBlockElevation(164),
                graded.auxiliaryPort(
                        ApproachDirection.WEST,
                        TrafficFlow.INBOUND,
                        448).elevation());
        assertEquals(
                new HalfBlockElevation(168),
                graded.auxiliaryPort(
                        ApproachDirection.NORTH,
                        TrafficFlow.OUTBOUND,
                        576).elevation());
    }

    @Test
    void everyTurningRampMeetsItsCollectorWithoutAHeightStep() {
        for (InterchangeDesign design : InterchangeCatalogue.builtIns()) {
            if (design.form() != JunctionForm.FOUR_WAY) {
                continue;
            }
            InterchangeGeometrySite site = design.type() == InterchangeType.STACK
                    ? STACK_SITE : GRADED_SITE;
            InterchangeLayout layout = factory.create(design, site);
            List<InterchangeConnection> turns = layout.connections().stream()
                    .filter(connection -> connection.form() != RampForm.MAINLINE)
                    .toList();
            for (InterchangeConnection turn : turns) {
                InterchangeAuxiliaryLane departure = layout.auxiliaryLanes().stream()
                        .filter(lane -> lane.mainlineMovement().from()
                                == turn.movement().from())
                        .findFirst()
                        .orElseThrow();
                var departurePoint = nearestPoint(
                        departure.route(), turn.route().centerline().startPose().point());
                assertTrue(departurePoint.distance() < 0.01);
                assertEquals(
                        departure.route().centerline().elevationAt(
                                departurePoint.stationBlocks()),
                        turn.route().centerline().startElevation(),
                        design.id() + " departure " + turn.movement());

                InterchangeAuxiliaryLane arrival = layout.auxiliaryLanes().stream()
                        .filter(lane -> lane.mainlineMovement().to()
                                == turn.movement().to())
                        .findFirst()
                        .orElseThrow();
                var arrivalPoint = nearestPoint(
                        arrival.route(), turn.route().centerline().endPose().point());
                assertTrue(arrivalPoint.distance() < 0.01);
                assertEquals(
                        arrival.route().centerline().elevationAt(
                                arrivalPoint.stationBlocks()),
                        turn.route().centerline().endElevation(),
                        design.id() + " arrival " + turn.movement());
            }
        }
    }

    @Test
    void stackTurnsShareOnePhysicalTrunkBeforeForksAndAfterMerges() {
        InterchangeDesign stack = InterchangeCatalogue.builtIns().stream()
                .filter(design -> design.type() == InterchangeType.STACK)
                .findFirst()
                .orElseThrow();
        InterchangeLayout layout = factory.create(stack, STACK_SITE);

        for (ApproachDirection approach : ApproachDirection.values()) {
            List<InterchangeConnection> departures = layout.connections().stream()
                    .filter(connection -> connection.form() != RampForm.MAINLINE)
                    .filter(connection -> connection.movement().from() == approach)
                    .toList();
            assertEquals(2, departures.size());
            assertPoint(
                    departures.get(0).route().centerline().startPose().point(),
                    departures.get(1).route().centerline().startPose().point());
            assertSharedStationFromStart(
                    departures.get(0).route(), departures.get(1).route(), 24.0);

            List<InterchangeConnection> arrivals = layout.connections().stream()
                    .filter(connection -> connection.form() != RampForm.MAINLINE)
                    .filter(connection -> connection.movement().to() == approach)
                    .toList();
            assertEquals(2, arrivals.size());
            assertPoint(
                    arrivals.get(0).route().centerline().endPose().point(),
                    arrivals.get(1).route().centerline().endPose().point());
            assertSharedStationFromEnd(
                    arrivals.get(0).route(), arrivals.get(1).route(), 24.0);
        }
    }

    @Test
    void stackRampsNeverCrushAnArterialInsideItsVehicleEnvelope() {
        InterchangeDesign stack = InterchangeCatalogue.builtIns().stream()
                .filter(design -> design.type() == InterchangeType.STACK)
                .findFirst()
                .orElseThrow();
        InterchangeLayout layout = factory.create(stack, STACK_SITE);
        List<GradedArterial> arterials = List.of(
                new GradedArterial(
                        RoadAxis.X,
                        (int) STACK_SITE.center().x(),
                        (int) STACK_SITE.center().z(),
                        STACK_SITE.approachRunBlocks(),
                        true,
                        true,
                        STACK_SITE.xRoadNativeElevation(),
                        STACK_SITE.xRoadCenterElevation()),
                new GradedArterial(
                        RoadAxis.Z,
                        (int) STACK_SITE.center().x(),
                        (int) STACK_SITE.center().z(),
                        STACK_SITE.approachRunBlocks(),
                        true,
                        true,
                        STACK_SITE.zRoadNativeElevation(),
                        STACK_SITE.zRoadCenterElevation()));
        List<String> conflicts = new ArrayList<>();

        layout.connections().stream()
                .filter(connection -> connection.form() != RampForm.MAINLINE)
                .forEach(connection -> connection.route().centerline().samples().forEach(sample -> {
                    for (GradedArterial arterial : arterials) {
                        int longitudinal = arterial.axis() == RoadAxis.X
                                ? (int) StrictMath.floor(sample.point().x())
                                : (int) StrictMath.floor(sample.point().z());
                        if (!arterial.containsLongitudinal(longitudinal)) {
                            continue;
                        }
                        double crossDistance = arterial.axis() == RoadAxis.X
                                ? StrictMath.abs(sample.point().z() - STACK_SITE.center().z())
                                : StrictMath.abs(sample.point().x() - STACK_SITE.center().x());
                        double overlapDistance = 16.0
                                + connection.route().widthBlocks() / 2.0;
                        if (crossDistance >= overlapDistance - TOLERANCE) {
                            continue;
                        }
                        int separation = StrictMath.abs(
                                sample.elevation().halfBlocks()
                                        - arterial.elevationAt(longitudinal).halfBlocks());
                        if (separation > 0
                                && separation < RoadDesignStandard.DEFAULT
                                        .minimumVehicleClearanceBlocks() * 2) {
                            conflicts.add(connection.movement() + " with " + arterial.axis()
                                    + " at station " + sample.stationBlocks()
                                    + " near " + sample.point()
                                    + " (ramp=" + sample.elevation().halfBlocks()
                                    + ", arterial="
                                    + arterial.elevationAt(longitudinal).halfBlocks()
                                    + ", crossDistance=" + crossDistance
                                    + ", separation=" + separation + " half-blocks)");
                            break;
                        }
                    }
                }));

        assertTrue(conflicts.isEmpty(), () -> conflicts.stream().limit(20).toList().toString());
    }

    @Test
    void stackRampOverlapsAreEitherJoinedOrFullyGradeSeparated() {
        InterchangeDesign stack = InterchangeCatalogue.builtIns().stream()
                .filter(design -> design.type() == InterchangeType.STACK)
                .findFirst()
                .orElseThrow();
        InterchangeLayout layout = factory.create(stack, STACK_SITE);
        List<InterchangeConnection> turns = layout.connections().stream()
                .filter(connection -> connection.form() != RampForm.MAINLINE)
                .toList();
        List<String> conflicts = new ArrayList<>();
        int minimumClearance = RoadDesignStandard.DEFAULT
                .minimumVehicleClearanceBlocks() * 2;

        for (int leftIndex = 0; leftIndex < turns.size(); leftIndex++) {
            InterchangeConnection leftConnection = turns.get(leftIndex);
            RampRoute left = leftConnection.route();
            for (int rightIndex = leftIndex + 1; rightIndex < turns.size(); rightIndex++) {
                InterchangeConnection rightConnection = turns.get(rightIndex);
                RampRoute right = rightConnection.route();
                double overlapDistance = (left.widthBlocks() + right.widthBlocks()) / 2.0 - 0.5;
                double overlapDistanceSquared = overlapDistance * overlapDistance;
                TerminalOverlap sharedDeparture = terminalOverlap(
                        left, right, overlapDistance, false);
                TerminalOverlap sharedArrival = terminalOverlap(
                        left, right, overlapDistance, true);
                boolean conflictFound = false;
                for (int leftSampleIndex = 0;
                        leftSampleIndex < left.centerline().samples().size();
                        leftSampleIndex += 2) {
                    var leftSample = left.centerline().samples().get(leftSampleIndex);
                    ClosestRoutePoint rightPoint = nearestPoint(
                            right, leftSample.point());
                    if (rightPoint.distance() * rightPoint.distance()
                            >= overlapDistanceSquared) {
                        continue;
                    }
                    int separation = StrictMath.abs(
                            leftSample.elevation().halfBlocks()
                                    - right.centerline().elevationAt(
                                            rightPoint.stationBlocks()).halfBlocks());
                    boolean declaredSharedTrunk = separation == 0
                            && ((leftConnection.movement().from()
                                    == rightConnection.movement().from()
                                    && leftSample.stationBlocks()
                                            <= sharedDeparture.leftBlocks() + TOLERANCE
                                    && rightPoint.stationBlocks()
                                            <= sharedDeparture.rightBlocks() + TOLERANCE)
                            || (leftConnection.movement().to()
                                    == rightConnection.movement().to()
                                    && left.centerline().lengthBlocks()
                                            - leftSample.stationBlocks()
                                            <= sharedArrival.leftBlocks() + TOLERANCE
                                    && right.centerline().lengthBlocks()
                                            - rightPoint.stationBlocks()
                                            <= sharedArrival.rightBlocks() + TOLERANCE));
                    if (separation < minimumClearance && !declaredSharedTrunk) {
                        conflicts.add("routes " + leftIndex + '/' + rightIndex
                                + " overlap at " + leftSample.point()
                                + " with " + separation + " half-blocks clearance");
                        conflictFound = true;
                    }
                    if (conflictFound) {
                        break;
                    }
                }
            }
        }

        assertTrue(conflicts.isEmpty(), () -> conflicts.toString());
    }

    @Test
    void builtInsAddOneContinuousAuxiliaryLanePerMainlineMovement() {
        for (InterchangeDesign design : InterchangeCatalogue.builtIns()) {
            InterchangeGeometrySite site = siteFor(design);
            InterchangeLayout layout = factory.create(design, site);
            long mainlines = layout.connections().stream()
                    .filter(connection -> connection.form() == RampForm.MAINLINE)
                    .count();

            assertEquals(mainlines, layout.auxiliaryLanes().size(), design.id().toString());
            for (InterchangeAuxiliaryLane lane : layout.auxiliaryLanes()) {
                assertEquals(10, lane.route().widthBlocks());
                assertPoint(
                        site.outerThroughPort(
                                lane.mainlineMovement().from(), TrafficFlow.INBOUND).point(),
                        lane.route().centerline().startPose().point());
                assertPoint(
                        site.outerThroughPort(
                                lane.mainlineMovement().to(), TrafficFlow.OUTBOUND).point(),
                        lane.route().centerline().endPose().point());
                double maximumCrossOffset = lane.route().centerline().samples().stream()
                        .mapToDouble(sample -> crossOffset(
                                lane.mainlineMovement().from(), sample.point()))
                        .max()
                        .orElseThrow();
                assertEquals(21.0, maximumCrossOffset, 0.01, design.id().toString());
            }
        }
    }

    @Test
    void routesStayInsideTheApproachEnvelopeAndAreDeterministic() {
        for (InterchangeDesign design : InterchangeCatalogue.builtIns()) {
            InterchangeLayout first = factory.create(design, siteFor(design));
            InterchangeLayout second = factory.create(design, siteFor(design));

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
            InterchangeLayout layout = factory.create(design, siteFor(design));
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

        InterchangeLayout layout = factory.create(design, CUSTOM_SITE);

        InterchangeConnection direct = connection(
                layout, ApproachDirection.WEST, ApproachDirection.SOUTH);
        assertEquals(RampForm.DIRECT, direct.form());
        assertEquals(RampControl.YIELD, direct.control());
        assertEquals(11, direct.route().widthBlocks());
        assertEquals(2, direct.structureLevel());

        InterchangeConnection upperTurn = connection(
                layout, ApproachDirection.NORTH, ApproachDirection.EAST);
        assertEquals(RampForm.DIRECT, upperTurn.form());
        assertEquals(RampControl.SIGNALIZED, upperTurn.control());
        assertEquals(8, upperTurn.route().widthBlocks());
        assertEquals(2, upperTurn.structureLevel());
        assertEquals(
                CUSTOM_SITE.zRoadCenterElevation(),
                direct.route().centerline().elevationAt(
                        direct.route().centerline().lengthBlocks() / 2.0),
                "structure_level 2 must occupy the upper physical tier in the core");

        assertExactPorts(layout);
    }

    @Test
    void rejectsCustomStructureLevelThatCannotBeReachedAtTheLegalGrade() {
        InterchangeDesign design = customFourWayDesign();
        InterchangeGeometrySite tooSteep = new InterchangeGeometrySite(
                new PlanarPoint(0.0, 0.0),
                352,
                new HalfBlockElevation(140),
                new HalfBlockElevation(240),
                new HalfBlockElevation(140),
                new HalfBlockElevation(240));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> factory.create(design, tooSteep));

        assertTrue(error.getMessage().contains("grade requires"), error::getMessage);
    }

    @Test
    void rotatesCanonicalThreeWayGeometryToTheSurveyedMissingApproach() {
        InterchangeDesign design = customThreeWayDesign();
        Set<ApproachDirection> approaches = EnumSet.of(
                ApproachDirection.NORTH,
                ApproachDirection.SOUTH,
                ApproachDirection.WEST);

        InterchangeLayout layout = factory.create(design, CUSTOM_SITE, approaches);

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
                    RampControl.FREE_FLOW, 8,
                    from == ApproachDirection.NORTH || from == ApproachDirection.SOUTH
                            ? 2 : 1));
            movements.add(blueprint(from, MovementKind.RIGHT, RampForm.DIRECT,
                    RampControl.YIELD, from == ApproachDirection.WEST ? 11 : 8, 2));
            movements.add(blueprint(from, MovementKind.LEFT,
                    RampForm.DIRECT,
                    RampControl.SIGNALIZED,
                    8,
                    2));
        }
        return design(
                "example:custom_four_way",
                InterchangeType.DIAMOND,
                JunctionForm.FOUR_WAY,
                2,
                false,
                false,
                4,
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
                        2));
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
                320,
                2,
                352,
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
                                + " / " + rightSample.point()
                                + " (stations=" + leftSample.stationBlocks()
                                + '/' + rightSample.stationBlocks() + ')'
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

    private static void assertSharedStationFromStart(
            RampRoute left,
            RampRoute right,
            double station) {
        var leftSample = nearestSample(left, station);
        var rightSample = nearestSample(right, station);
        assertTrue(distance(leftSample.point(), rightSample.point()) < 1.0,
                () -> "shared departure trunk separated by "
                        + distance(leftSample.point(), rightSample.point()) + " blocks");
        assertEquals(leftSample.elevation(), rightSample.elevation());
    }

    private static void assertSharedStationFromEnd(
            RampRoute left,
            RampRoute right,
            double distanceFromEnd) {
        var leftSample = nearestSample(
                left, left.centerline().lengthBlocks() - distanceFromEnd);
        var rightSample = nearestSample(
                right, right.centerline().lengthBlocks() - distanceFromEnd);
        assertTrue(distance(leftSample.point(), rightSample.point()) < 1.0,
                () -> "shared arrival trunk separated by "
                        + distance(leftSample.point(), rightSample.point()) + " blocks");
        assertEquals(leftSample.elevation(), rightSample.elevation());
    }

    private static net.austizz.lostcitiesroadfixes.interchange.geometry.RampCenterlineSample
            nearestSample(RampRoute route, double station) {
        return route.centerline().samples().stream()
                .min(java.util.Comparator.comparingDouble(sample ->
                        StrictMath.abs(sample.stationBlocks() - station)))
                .orElseThrow();
    }

    private static ClosestRoutePoint nearestPoint(RampRoute route, PlanarPoint point) {
        ClosestRoutePoint closest = null;
        List<net.austizz.lostcitiesroadfixes.interchange.geometry.RampCenterlineSample> samples =
                route.centerline().samples();
        for (int index = 0; index < samples.size() - 1; index++) {
            var start = samples.get(index);
            var end = samples.get(index + 1);
            double segmentX = end.point().x() - start.point().x();
            double segmentZ = end.point().z() - start.point().z();
            double lengthSquared = segmentX * segmentX + segmentZ * segmentZ;
            if (lengthSquared == 0.0) {
                continue;
            }
            double fraction = ((point.x() - start.point().x()) * segmentX
                    + (point.z() - start.point().z()) * segmentZ) / lengthSquared;
            fraction = StrictMath.max(0.0, StrictMath.min(1.0, fraction));
            PlanarPoint projected = new PlanarPoint(
                    start.point().x() + segmentX * fraction,
                    start.point().z() + segmentZ * fraction);
            double station = start.stationBlocks()
                    + (end.stationBlocks() - start.stationBlocks()) * fraction;
            ClosestRoutePoint candidate = new ClosestRoutePoint(
                    station, distance(projected, point));
            if (closest == null || candidate.distance() < closest.distance()) {
                closest = candidate;
            }
        }
        if (closest == null) {
            throw new IllegalStateException("Route has no projectable centerline segment");
        }
        return closest;
    }

    private static TerminalOverlap terminalOverlap(
            RampRoute left,
            RampRoute right,
            double overlapDistance,
            boolean arrival) {
        double leftBlocks = 0.0;
        double rightBlocks = 0.0;
        List<net.austizz.lostcitiesroadfixes.interchange.geometry.RampCenterlineSample> samples =
                left.centerline().samples();
        for (int offset = 0; offset < samples.size(); offset++) {
            int index = arrival ? samples.size() - 1 - offset : offset;
            var sample = samples.get(index);
            ClosestRoutePoint nearest = nearestPoint(right, sample.point());
            if (nearest.distance() >= overlapDistance) {
                break;
            }
            leftBlocks = arrival
                    ? left.centerline().lengthBlocks() - sample.stationBlocks()
                    : sample.stationBlocks();
            rightBlocks = arrival
                    ? right.centerline().lengthBlocks() - nearest.stationBlocks()
                    : nearest.stationBlocks();
        }
        return new TerminalOverlap(leftBlocks, rightBlocks);
    }

    private record ClosestRoutePoint(double stationBlocks, double distance) {
    }

    private record TerminalOverlap(double leftBlocks, double rightBlocks) {
    }

    private static double crossOffset(
            ApproachDirection direction,
            PlanarPoint point) {
        return switch (direction) {
            case EAST, WEST -> StrictMath.abs(point.z() - SITE.center().z());
            case NORTH, SOUTH -> StrictMath.abs(point.x() - SITE.center().x());
        };
    }

    private static InterchangeGeometrySite siteFor(InterchangeDesign design) {
        return design.type() == InterchangeType.STACK ? STACK_SITE : SITE;
    }

    private static void assertPoint(PlanarPoint expected, PlanarPoint actual) {
        assertEquals(expected.x(), actual.x(), TOLERANCE);
        assertEquals(expected.z(), actual.z(), TOLERANCE);
    }
}
