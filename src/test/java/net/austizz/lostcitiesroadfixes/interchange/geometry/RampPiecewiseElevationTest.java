package net.austizz.lostcitiesroadfixes.interchange.geometry;

import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RampPiecewiseElevationTest {
    @Test
    void risesToAnAdjustedCenterAndReturnsToNativeElevation() {
        RampPathBuilder builder = new RampPathBuilder(
                RoadDesignStandard.DEFAULT,
                new PlanarPoint(0.0, 0.0),
                RoadHeading.EAST)
                .straight(320);
        RampCenterline centerline = builder.build(List.of(
                keyframe(0, 152),
                keyframe(160, 160),
                keyframe(320, 152)));

        assertEquals(elevation(152), centerline.elevationAt(0));
        assertEquals(elevation(156), centerline.elevationAt(80));
        assertEquals(elevation(160), centerline.elevationAt(160));
        assertEquals(elevation(156), centerline.elevationAt(240));
        assertEquals(elevation(152), centerline.elevationAt(320));
        assertEquals(3, centerline.elevationProfile().size());
    }

    @Test
    void validatesEveryPiecewiseGradeLeg() {
        RampPathBuilder builder = new RampPathBuilder(
                RoadDesignStandard.DEFAULT,
                new PlanarPoint(0.0, 0.0),
                RoadHeading.EAST)
                .straight(128);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> builder.build(List.of(
                        keyframe(0, 140),
                        keyframe(63, 148),
                        keyframe(128, 148))));

        assertTrue(failure.getMessage().contains("64"));
        assertTrue(failure.getMessage().contains("63"));
    }

    private static RampElevationKeyframe keyframe(double station, int halfBlocks) {
        return new RampElevationKeyframe(station, elevation(halfBlocks));
    }

    private static HalfBlockElevation elevation(int halfBlocks) {
        return new HalfBlockElevation(halfBlocks);
    }
}
