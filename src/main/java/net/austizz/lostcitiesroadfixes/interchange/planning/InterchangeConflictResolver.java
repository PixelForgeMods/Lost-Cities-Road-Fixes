package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.planning.continuity.ChunkBounds;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Selects a bounded hard-core set from independently feasible interchange sites.
 * A candidate is admitted only when no directly conflicting candidate has a
 * higher stable priority. This local rule needs only one finite survey halo and
 * therefore gives the same result on both sides of a planning-region boundary.
 */
public final class InterchangeConflictResolver {
    private static final int BLOCKS_PER_CHUNK = 16;

    private final int reservationPaddingBlocks;
    private final int maximumReservationRadiusBlocks;
    private final int surveyMarginChunks;

    public InterchangeConflictResolver(RoadDesignStandard standard) {
        Objects.requireNonNull(standard, "standard");
        reservationPaddingBlocks = Math.floorDiv(
                standard.arterialCrossSection().totalWidthBlocks(), 2);
        maximumReservationRadiusBlocks = Math.addExact(
                RoadCrossingSurveyor.MAXIMUM_RADIUS_BLOCKS,
                reservationPaddingBlocks);
        surveyMarginChunks = ceilDiv(
                Math.multiplyExact(maximumReservationRadiusBlocks, 2),
                BLOCKS_PER_CHUNK);
    }

    public InterchangeConflictResolution resolve(
            Collection<PlannedInterchange> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        List<PlannedInterchange> ordered = List.copyOf(candidates);
        Set<ChunkPoint> chunks = new HashSet<>();
        for (PlannedInterchange candidate : ordered) {
            Objects.requireNonNull(candidate, "candidate");
            if (!chunks.add(candidate.crossing().chunk())) {
                throw new IllegalArgumentException(
                        "Duplicate interchange candidate " + candidate.crossing().chunk());
            }
        }

        List<PlannedInterchange> selected = new ArrayList<>();
        List<ConflictedRoadCrossing> conflicts = new ArrayList<>();
        for (PlannedInterchange candidate : ordered) {
            PlannedInterchange blocker = ordered.stream()
                    .filter(other -> other != candidate)
                    .filter(other -> conflicts(candidate, other))
                    .filter(other -> comparePriority(other, candidate) < 0)
                    .min(this::comparePriority)
                    .orElse(null);
            if (blocker == null) {
                selected.add(candidate);
                continue;
            }
            conflicts.add(new ConflictedRoadCrossing(
                    candidate.crossing(),
                    candidate.decision(),
                    blocker.crossing().chunk(),
                    Math.addExact(
                            reservationRadiusBlocks(candidate),
                            reservationRadiusBlocks(blocker))));
        }
        return new InterchangeConflictResolution(selected, conflicts);
    }

    public boolean conflicts(PlannedInterchange left, PlannedInterchange right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.crossing().chunk().equals(right.crossing().chunk())) {
            throw new IllegalArgumentException("A crossing cannot conflict with itself");
        }
        long centerDistanceX = Math.multiplyExact(
                StrictMath.abs((long) left.crossing().chunk().x()
                        - right.crossing().chunk().x()),
                BLOCKS_PER_CHUNK);
        long centerDistanceZ = Math.multiplyExact(
                StrictMath.abs((long) left.crossing().chunk().z()
                        - right.crossing().chunk().z()),
                BLOCKS_PER_CHUNK);
        long requiredSeparation = (long) reservationRadiusBlocks(left)
                + reservationRadiusBlocks(right);
        return centerDistanceX < requiredSeparation
                && centerDistanceZ < requiredSeparation;
    }

    public int reservationRadiusBlocks(PlannedInterchange interchange) {
        Objects.requireNonNull(interchange, "interchange");
        int radius = Math.addExact(
                interchange.decision().selected().orElseThrow().minimumRadiusBlocks(),
                reservationPaddingBlocks);
        if (radius > maximumReservationRadiusBlocks) {
            throw new IllegalArgumentException(
                    "Selected interchange radius exceeds the surveyed site maximum");
        }
        return radius;
    }

    public int surveyMarginChunks() {
        return surveyMarginChunks;
    }

    public ChunkBounds surveyBounds(ChunkBounds ownershipBounds) {
        Objects.requireNonNull(ownershipBounds, "ownershipBounds");
        return new ChunkBounds(
                Math.subtractExact(ownershipBounds.minX(), surveyMarginChunks),
                Math.addExact(ownershipBounds.maxX(), surveyMarginChunks),
                Math.subtractExact(ownershipBounds.minZ(), surveyMarginChunks),
                Math.addExact(ownershipBounds.maxZ(), surveyMarginChunks));
    }

    private int comparePriority(PlannedInterchange left, PlannedInterchange right) {
        int byDemand = Integer.compare(
                right.crossing().demand().ordinal(),
                left.crossing().demand().ordinal());
        if (byDemand != 0) {
            return byDemand;
        }
        int byApproaches = Integer.compare(
                right.crossing().approaches().size(),
                left.crossing().approaches().size());
        if (byApproaches != 0) {
            return byApproaches;
        }
        int byCompactness = Integer.compare(
                reservationRadiusBlocks(left),
                reservationRadiusBlocks(right));
        if (byCompactness != 0) {
            return byCompactness;
        }
        int bySeed = Long.compareUnsigned(
                left.crossing().selectionSeed(),
                right.crossing().selectionSeed());
        return bySeed != 0
                ? bySeed
                : left.crossing().compareTo(right.crossing());
    }

    private static int ceilDiv(int dividend, int divisor) {
        return dividend == 0 ? 0 : 1 + (dividend - 1) / divisor;
    }
}
