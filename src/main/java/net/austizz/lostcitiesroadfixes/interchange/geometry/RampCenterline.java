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
        List<RampElevationKeyframe> elevationProfile,
        List<RampCenterlineSample> samples) {
    public RampCenterline {
        Objects.requireNonNull(startPose, "startPose");
        Objects.requireNonNull(endPose, "endPose");
        Objects.requireNonNull(startElevation, "startElevation");
        Objects.requireNonNull(endElevation, "endElevation");
        if (!Double.isFinite(lengthBlocks) || lengthBlocks <= 0.0) {
            throw new IllegalArgumentException("Centerline length must be finite and positive");
        }
        elevationProfile = List.copyOf(elevationProfile);
        if (elevationProfile.size() < 2
                || elevationProfile.getFirst().stationBlocks() != 0.0
                || elevationProfile.getLast().stationBlocks() != lengthBlocks) {
            throw new IllegalArgumentException(
                    "Elevation profile must include exact centerline endpoints");
        }
        double previousKeyframe = -1.0;
        for (RampElevationKeyframe keyframe : elevationProfile) {
            if (keyframe.stationBlocks() <= previousKeyframe) {
                throw new IllegalArgumentException("Elevation keyframe stations must increase");
            }
            previousKeyframe = keyframe.stationBlocks();
        }
        if (!elevationProfile.getFirst().elevation().equals(startElevation)
                || !elevationProfile.getLast().elevation().equals(endElevation)) {
            throw new IllegalArgumentException("Elevation profile endpoints do not match centerline endpoints");
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
        return elevationAt(elevationProfile, stationBlocks, lengthBlocks);
    }

    public RampCenterline withElevationProfile(
            List<RampElevationKeyframe> replacementProfile) {
        List<RampElevationKeyframe> profile = List.copyOf(replacementProfile);
        List<RampCenterlineSample> reprofiled = samples.stream()
                .map(sample -> new RampCenterlineSample(
                        sample.stationBlocks(),
                        sample.point(),
                        elevationAt(profile, sample.stationBlocks(), lengthBlocks),
                        sample.headingRadians()))
                .toList();
        return new RampCenterline(
                startPose,
                endPose,
                lengthBlocks,
                profile.getFirst().elevation(),
                profile.getLast().elevation(),
                profile,
                reprofiled);
    }

    static HalfBlockElevation elevationAt(
            List<RampElevationKeyframe> profile,
            double stationBlocks,
            double lengthBlocks) {
        if (stationBlocks == lengthBlocks) {
            return profile.getLast().elevation();
        }
        for (int index = 0; index < profile.size() - 1; index++) {
            RampElevationKeyframe start = profile.get(index);
            RampElevationKeyframe end = profile.get(index + 1);
            if (stationBlocks <= end.stationBlocks()) {
                double localStation = stationBlocks - start.stationBlocks();
                double localLength = end.stationBlocks() - start.stationBlocks();
                long delta = (long) end.elevation().halfBlocks()
                        - start.elevation().halfBlocks();
                long progress = (long) StrictMath.floor(
                        localStation * Math.abs(delta) / localLength + 1.0e-12);
                long elevation = delta >= 0
                        ? (long) start.elevation().halfBlocks() + progress
                        : (long) start.elevation().halfBlocks() - progress;
                return new HalfBlockElevation(Math.toIntExact(elevation));
            }
        }
        throw new IllegalStateException("No elevation profile leg contains station " + stationBlocks);
    }
}
