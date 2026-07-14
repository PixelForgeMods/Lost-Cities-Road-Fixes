package net.austizz.lostcitiesroadfixes.interchange.geometry;

import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;

import java.util.List;
import java.util.Objects;

public record RampCenterline(
        RampPose startPose,
        RampPose endPose,
        double lengthBlocks,
        HalfBlockElevation startElevation,
        HalfBlockElevation endElevation,
        List<RampCenterlineSample> samples) {
    public RampCenterline {
        Objects.requireNonNull(startPose, "startPose");
        Objects.requireNonNull(endPose, "endPose");
        Objects.requireNonNull(startElevation, "startElevation");
        Objects.requireNonNull(endElevation, "endElevation");
        if (!Double.isFinite(lengthBlocks) || lengthBlocks <= 0.0) {
            throw new IllegalArgumentException("Centerline length must be finite and positive");
        }
        samples = List.copyOf(samples);
        if (samples.size() < 2) {
            throw new IllegalArgumentException("A centerline needs at least two samples");
        }
        double previous = -1.0;
        for (RampCenterlineSample sample : samples) {
            if (sample.stationBlocks() <= previous) {
                throw new IllegalArgumentException("Centerline sample stations must increase");
            }
            previous = sample.stationBlocks();
        }
        if (samples.getFirst().stationBlocks() != 0.0
                || samples.getLast().stationBlocks() != lengthBlocks) {
            throw new IllegalArgumentException("Centerline samples must include exact route endpoints");
        }
    }

    public HalfBlockElevation elevationAt(double stationBlocks) {
        if (!Double.isFinite(stationBlocks)
                || stationBlocks < 0.0
                || stationBlocks > lengthBlocks) {
            throw new IllegalArgumentException(
                    "Station must be between 0 and " + lengthBlocks + ": " + stationBlocks);
        }
        if (stationBlocks == lengthBlocks) {
            return endElevation;
        }

        long delta = (long) endElevation.halfBlocks() - startElevation.halfBlocks();
        long magnitude = Math.abs(delta);
        long progress = (long) StrictMath.floor(stationBlocks * magnitude / lengthBlocks + 1.0e-12);
        long elevation = delta >= 0
                ? (long) startElevation.halfBlocks() + progress
                : (long) startElevation.halfBlocks() - progress;
        return new HalfBlockElevation(Math.toIntExact(elevation));
    }
}
