package net.austizz.lostcitiesroadfixes.interchange.geometry;

import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RampPathBuilderTest {
    private static final double TOLERANCE = 1.0e-9;

    @Test
    void composesExactLeftAndRightCircularTurns() {
        RampCenterline left = builder(RoadHeading.EAST)
                .turnLeft(24, 90)
                .build(elevation(140), elevation(140));
        RampCenterline right = builder(RoadHeading.EAST)
                .turnRight(24, 90)
                .build(elevation(140), elevation(140));

        assertPoint(24.0, -24.0, left.endPose().point());
        assertEquals(RoadHeading.NORTH.radians(), left.endPose().headingRadians(), TOLERANCE);
        assertPoint(24.0, 24.0, right.endPose().point());
        assertEquals(RoadHeading.SOUTH.radians(), right.endPose().headingRadians(), TOLERANCE);
    }

    @Test
    void rejectsCurvesBelowTheRampDesignRadius() {
        RampPathBuilder builder = builder(RoadHeading.EAST);

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> builder.turnLeft(23, 90));

        assertTrue(failure.getMessage().contains("24"));
    }

    @Test
    void acceptsTheExactHalfBlockGradeBoundary() {
        RampCenterline route = builder(RoadHeading.EAST)
                .straight(160)
                .build(elevation(140), elevation(160));

        assertEquals(160.0, route.lengthBlocks(), TOLERANCE);
        assertEquals(elevation(140), route.elevationAt(0));
        assertEquals(elevation(160), route.elevationAt(160));
        for (int station = 8; station <= 160; station += 8) {
            int rise = route.elevationAt(station).halfBlocks()
                    - route.elevationAt(station - 8).halfBlocks();
            assertTrue(rise <= 1, "rise at station " + station + " was " + rise);
        }
    }

    @Test
    void rejectsAPathOneBlockShortOfTheRequiredGradeRun() {
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> builder(RoadHeading.EAST)
                        .straight(159)
                        .build(elevation(140), elevation(160)));

        assertTrue(failure.getMessage().contains("160"));
        assertTrue(failure.getMessage().contains("159"));
    }

    @Test
    void producesStableSamplesForTheSameRecipe() {
        RampCenterline first = recipe();
        RampCenterline second = recipe();

        assertEquals(first, second);
        assertEquals(first.samples(), second.samples());
    }

    private static RampCenterline recipe() {
        return builder(RoadHeading.WEST)
                .straight(32)
                .turnRight(32, 180)
                .straight(64)
                .build(elevation(120), elevation(128));
    }

    private static RampPathBuilder builder(RoadHeading heading) {
        return new RampPathBuilder(
                RoadDesignStandard.DEFAULT,
                new PlanarPoint(0.0, 0.0),
                heading);
    }

    private static HalfBlockElevation elevation(int halfBlocks) {
        return new HalfBlockElevation(halfBlocks);
    }

    private static void assertPoint(double x, double z, PlanarPoint actual) {
        assertEquals(x, actual.x(), TOLERANCE);
        assertEquals(z, actual.z(), TOLERANCE);
    }
}
