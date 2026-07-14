package net.austizz.lostcitiesroadfixes.interchange.geometry;

import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;

import java.util.Objects;

public record RampElevationKeyframe(
        double stationBlocks,
        HalfBlockElevation elevation) {
    public RampElevationKeyframe {
        if (!Double.isFinite(stationBlocks) || stationBlocks < 0.0) {
            throw new IllegalArgumentException("Elevation keyframe station must be finite and non-negative");
        }
        Objects.requireNonNull(elevation, "elevation");
    }
}
