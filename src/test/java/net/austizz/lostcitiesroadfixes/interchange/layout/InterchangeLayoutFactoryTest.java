package net.austizz.lostcitiesroadfixes.interchange.layout;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeCatalogue;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesign;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeType;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.geometry.PlanarPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
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

    private static void assertPoint(PlanarPoint expected, PlanarPoint actual) {
        assertEquals(expected.x(), actual.x(), TOLERANCE);
        assertEquals(expected.z(), actual.z(), TOLERANCE);
    }
}
