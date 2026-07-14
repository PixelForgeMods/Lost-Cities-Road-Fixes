package net.austizz.lostcitiesroadfixes.interchange.geometry;

import net.austizz.lostcitiesroadfixes.planning.elevation.GradeProfilePlanner;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import net.austizz.lostcitiesroadfixes.road.RoadKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RampPathBuilder {
    private static final double EPSILON = 1.0e-9;

    private final RoadDesignStandard standard;
    private final GradeProfilePlanner gradePlanner;
    private final RampPose startPose;
    private final List<PathSegment> segments = new ArrayList<>();
    private RampPose currentPose;
    private double lengthBlocks;

    public RampPathBuilder(
            RoadDesignStandard standard,
            PlanarPoint start,
            RoadHeading heading) {
        this.standard = Objects.requireNonNull(standard, "standard");
        this.gradePlanner = new GradeProfilePlanner(standard);
        this.startPose = new RampPose(
                Objects.requireNonNull(start, "start"),
                Objects.requireNonNull(heading, "heading").radians());
        this.currentPose = startPose;
    }

    public RampPathBuilder straight(double blocks) {
        requirePositiveFinite(blocks, "Straight length");
        StraightSegment segment = new StraightSegment(currentPose, blocks);
        append(segment);
        return this;
    }

    public RampPathBuilder turnLeft(double radiusBlocks, double degrees) {
        return turn(radiusBlocks, degrees, -1);
    }

    public RampPathBuilder turnRight(double radiusBlocks, double degrees) {
        return turn(radiusBlocks, degrees, 1);
    }

    public RampCenterline build(
            HalfBlockElevation startElevation,
            HalfBlockElevation endElevation) {
        Objects.requireNonNull(startElevation, "startElevation");
        Objects.requireNonNull(endElevation, "endElevation");
        if (segments.isEmpty()) {
            throw new IllegalStateException("A ramp path needs at least one segment");
        }

        int minimumRun = gradePlanner.minimumRunBlocks(startElevation, endElevation);
        if (lengthBlocks + EPSILON < minimumRun) {
            throw new IllegalArgumentException(
                    "Elevation transition requires " + minimumRun
                            + " blocks but the ramp path has only " + formatted(lengthBlocks));
        }

        List<RampCenterlineSample> samples = new ArrayList<>();
        int wholeStations = (int) StrictMath.floor(lengthBlocks);
        for (int station = 0; station <= wholeStations; station++) {
            addSample(samples, station, startElevation, endElevation);
        }
        if (lengthBlocks - wholeStations > EPSILON) {
            addSample(samples, lengthBlocks, startElevation, endElevation);
        }

        return new RampCenterline(
                startPose,
                currentPose,
                lengthBlocks,
                startElevation,
                endElevation,
                samples);
    }

    private RampPathBuilder turn(double radiusBlocks, double degrees, int direction) {
        requirePositiveFinite(radiusBlocks, "Turn radius");
        requirePositiveFinite(degrees, "Turn angle");
        if (degrees > 360.0) {
            throw new IllegalArgumentException("Turn angle cannot exceed 360 degrees");
        }
        standard.requireCurveRadius(RoadKind.RAMP, (int) StrictMath.floor(radiusBlocks));
        ArcSegment segment = new ArcSegment(
                currentPose,
                radiusBlocks,
                StrictMath.toRadians(degrees),
                direction);
        append(segment);
        return this;
    }

    private void append(PathSegment segment) {
        segments.add(segment);
        currentPose = segment.endPose();
        lengthBlocks += segment.lengthBlocks();
    }

    private void addSample(
            List<RampCenterlineSample> samples,
            double station,
            HalfBlockElevation startElevation,
            HalfBlockElevation endElevation) {
        SegmentLocation location = locate(station);
        HalfBlockElevation elevation = elevationAt(
                station, startElevation, endElevation, lengthBlocks);
        samples.add(new RampCenterlineSample(
                station,
                location.segment().pointAt(location.localStation()),
                elevation,
                location.segment().headingAt(location.localStation())));
    }

    private SegmentLocation locate(double station) {
        double segmentStart = 0.0;
        for (int index = 0; index < segments.size(); index++) {
            PathSegment segment = segments.get(index);
            double segmentEnd = segmentStart + segment.lengthBlocks();
            if (station <= segmentEnd + EPSILON || index == segments.size() - 1) {
                double local = StrictMath.max(0.0, StrictMath.min(
                        segment.lengthBlocks(), station - segmentStart));
                return new SegmentLocation(segment, local);
            }
            segmentStart = segmentEnd;
        }
        throw new IllegalStateException("Could not locate station " + station);
    }

    private static HalfBlockElevation elevationAt(
            double station,
            HalfBlockElevation start,
            HalfBlockElevation end,
            double length) {
        if (station >= length - EPSILON) {
            return end;
        }
        long delta = (long) end.halfBlocks() - start.halfBlocks();
        long progress = (long) StrictMath.floor(
                station * Math.abs(delta) / length + 1.0e-12);
        long halfBlocks = delta >= 0
                ? (long) start.halfBlocks() + progress
                : (long) start.halfBlocks() - progress;
        return new HalfBlockElevation(Math.toIntExact(halfBlocks));
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }

    private static String formatted(double value) {
        return value == StrictMath.rint(value)
                ? Long.toString((long) value)
                : String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private interface PathSegment {
        double lengthBlocks();

        PlanarPoint pointAt(double station);

        double headingAt(double station);

        RampPose endPose();
    }

    private record StraightSegment(RampPose startPose, double lengthBlocks) implements PathSegment {
        @Override
        public PlanarPoint pointAt(double station) {
            return new PlanarPoint(
                    startPose.point().x() + StrictMath.cos(startPose.headingRadians()) * station,
                    startPose.point().z() + StrictMath.sin(startPose.headingRadians()) * station);
        }

        @Override
        public double headingAt(double station) {
            return startPose.headingRadians();
        }

        @Override
        public RampPose endPose() {
            return new RampPose(pointAt(lengthBlocks), startPose.headingRadians());
        }
    }

    private record ArcSegment(
            RampPose startPose,
            double radiusBlocks,
            double sweepRadians,
            int direction) implements PathSegment {
        @Override
        public double lengthBlocks() {
            return radiusBlocks * sweepRadians;
        }

        @Override
        public PlanarPoint pointAt(double station) {
            double startHeading = startPose.headingRadians();
            double centerX = startPose.point().x()
                    - direction * StrictMath.sin(startHeading) * radiusBlocks;
            double centerZ = startPose.point().z()
                    + direction * StrictMath.cos(startHeading) * radiusBlocks;
            double radialX = direction * StrictMath.sin(startHeading) * radiusBlocks;
            double radialZ = -direction * StrictMath.cos(startHeading) * radiusBlocks;
            double angle = direction * station / radiusBlocks;
            double cosine = StrictMath.cos(angle);
            double sine = StrictMath.sin(angle);
            return new PlanarPoint(
                    centerX + radialX * cosine - radialZ * sine,
                    centerZ + radialX * sine + radialZ * cosine);
        }

        @Override
        public double headingAt(double station) {
            return new RampPose(startPose.point(),
                    startPose.headingRadians() + direction * station / radiusBlocks)
                    .headingRadians();
        }

        @Override
        public RampPose endPose() {
            return new RampPose(pointAt(lengthBlocks()), headingAt(lengthBlocks()));
        }
    }

    private record SegmentLocation(PathSegment segment, double localStation) {
    }
}
