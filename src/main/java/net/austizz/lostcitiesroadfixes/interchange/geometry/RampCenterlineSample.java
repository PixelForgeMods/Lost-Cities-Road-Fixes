package net.austizz.lostcitiesroadfixes.interchange.geometry;

import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;

import java.util.Objects;

public record RampCenterlineSample(
        double stationBlocks,
        PlanarPoint point,
        HalfBlockElevation elevation,
        double headingRadians) {
    public RampCenterlineSample {
        if (!Double.isFinite(stationBlocks) || stationBlocks < 0.0) {
            throw new IllegalArgumentException("Centerline station must be finite and non-negative");
        }
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(elevation, "elevation");
        if (!Double.isFinite(headingRadians)) {
            throw new IllegalArgumentException("Centerline heading must be finite");
        }
    }
}
