package net.austizz.lostcitiesroadfixes.planning.elevation;

import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class GradeProfilePlanner {
    private final RoadDesignStandard standard;

    public GradeProfilePlanner(RoadDesignStandard standard) {
        this.standard = Objects.requireNonNull(standard, "standard");
    }

    public int minimumRunBlocks(HalfBlockElevation start, HalfBlockElevation end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        long rise = Math.abs((long) end.halfBlocks() - start.halfBlocks());
        long scaledRun = Math.multiplyExact(rise, standard.gradeRunBlocks());
        return Math.toIntExact(ceilDiv(scaledRun, standard.maximumRiseHalfBlocks()));
    }

    /**
     * Returns the elevation reached after travelling inward from the native road
     * along the shortest grade permitted by this design standard. Once that
     * minimum transition is complete, the profile remains at the target
     * elevation.
     */
    public HalfBlockElevation elevationOnMinimumRun(
            HalfBlockElevation start,
            HalfBlockElevation end,
            int inwardDistanceBlocks) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (inwardDistanceBlocks < 0) {
            throw new IllegalArgumentException("Inward distance cannot be negative");
        }

        int minimumRun = minimumRunBlocks(start, end);
        if (minimumRun == 0 || inwardDistanceBlocks >= minimumRun) {
            return end;
        }
        return elevationAt(start, end, minimumRun, inwardDistanceBlocks);
    }

    public GradePlanResult plan(
            HalfBlockElevation start,
            HalfBlockElevation end,
            int horizontalRunBlocks) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (horizontalRunBlocks < 0) {
            throw new IllegalArgumentException("Horizontal run cannot be negative");
        }

        int minimumRun = minimumRunBlocks(start, end);
        if (horizontalRunBlocks < minimumRun) {
            return new GradePlanResult(
                    minimumRun,
                    horizontalRunBlocks,
                    Optional.empty(),
                    "Elevation transition needs at least " + minimumRun
                            + " horizontal blocks but only " + horizontalRunBlocks + " are available");
        }

        ElevationProfile profile = new ElevationProfile(samples(start, end, horizontalRunBlocks));
        return new GradePlanResult(
                minimumRun,
                horizontalRunBlocks,
                Optional.of(profile),
                "Elevation transition is driveable");
    }

    private static List<HalfBlockElevation> samples(
            HalfBlockElevation start,
            HalfBlockElevation end,
            int run) {
        List<HalfBlockElevation> samples = new ArrayList<>(run + 1);

        for (int distance = 0; distance <= run; distance++) {
            samples.add(elevationAt(start, end, run, distance));
        }
        return samples;
    }

    private static HalfBlockElevation elevationAt(
            HalfBlockElevation start,
            HalfBlockElevation end,
            int run,
            int distance) {
        long delta = (long) end.halfBlocks() - start.halfBlocks();
        long magnitude = Math.abs(delta);
        long halfBlocks;
        if (delta >= 0) {
            long progress = run == 0 ? 0 : (long) distance * magnitude / run;
            halfBlocks = (long) start.halfBlocks() + progress;
        } else {
            long remaining = run == 0 ? 0 : (long) (run - distance) * magnitude / run;
            halfBlocks = (long) end.halfBlocks() + remaining;
        }
        return new HalfBlockElevation(Math.toIntExact(halfBlocks));
    }

    private static long ceilDiv(long dividend, long divisor) {
        return dividend == 0 ? 0 : 1 + (dividend - 1) / divisor;
    }
}
