package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.planning.continuity.ChunkBounds;
import net.austizz.lostcitiesroadfixes.interchange.layout.ApproachDirection;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
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
    private static final int MAXIMUM_COMPILED_APPROACH_BLOCKS = 512;

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
                Math.multiplyExact(MAXIMUM_COMPILED_APPROACH_BLOCKS, 2),
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
        CompiledEnvelope leftEnvelope = envelope(left);
        CompiledEnvelope rightEnvelope = envelope(right);
        if (leftEnvelope.core().overlaps(rightEnvelope.core())) {
            return true;
        }

        if (leftEnvelope.centerZ() == rightEnvelope.centerZ()
                && connects(left, right, RoadAxis.X)
                && leftEnvelope.xCorridor().overlaps(rightEnvelope.xCorridor())) {
            return !compatibleSharedGrade(left, right, RoadAxis.X);
        }
        if (leftEnvelope.centerX() == rightEnvelope.centerX()
                && connects(left, right, RoadAxis.Z)
                && leftEnvelope.zCorridor().overlaps(rightEnvelope.zCorridor())) {
            return !compatibleSharedGrade(left, right, RoadAxis.Z);
        }

        return anyEnvelopeComponentOverlaps(leftEnvelope, rightEnvelope);
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

    private CompiledEnvelope envelope(PlannedInterchange interchange) {
        DetectedRoadCrossing crossing = interchange.crossing();
        long centerX = Math.addExact(
                Math.multiplyExact((long) crossing.chunk().x(), BLOCKS_PER_CHUNK), 8L);
        long centerZ = Math.addExact(
                Math.multiplyExact((long) crossing.chunk().z(), BLOCKS_PER_CHUNK), 8L);
        int coreRadius = reservationRadiusBlocks(interchange);
        int approach = interchange.decision().selectedApproachRunBlocks();
        int corridorHalfWidth = Math.addExact(reservationPaddingBlocks, 10);
        return new CompiledEnvelope(
                centerX,
                centerZ,
                Rectangle.around(centerX, centerZ, coreRadius, coreRadius),
                corridor(crossing, centerX, centerZ, approach, corridorHalfWidth, RoadAxis.X),
                corridor(crossing, centerX, centerZ, approach, corridorHalfWidth, RoadAxis.Z));
    }

    private static Rectangle corridor(
            DetectedRoadCrossing crossing,
            long centerX,
            long centerZ,
            int approach,
            int halfWidth,
            RoadAxis axis) {
        if (axis == RoadAxis.X) {
            long minimum = crossing.approaches().contains(ApproachDirection.WEST)
                    ? centerX - approach : centerX;
            long maximum = crossing.approaches().contains(ApproachDirection.EAST)
                    ? centerX + approach : centerX;
            return new Rectangle(
                    minimum, maximum, centerZ - halfWidth, centerZ + halfWidth);
        }
        long minimum = crossing.approaches().contains(ApproachDirection.NORTH)
                ? centerZ - approach : centerZ;
        long maximum = crossing.approaches().contains(ApproachDirection.SOUTH)
                ? centerZ + approach : centerZ;
        return new Rectangle(
                centerX - halfWidth, centerX + halfWidth, minimum, maximum);
    }

    private static boolean connects(
            PlannedInterchange left,
            PlannedInterchange right,
            RoadAxis axis) {
        DetectedRoadCrossing a = left.crossing();
        DetectedRoadCrossing b = right.crossing();
        if (axis == RoadAxis.X) {
            DetectedRoadCrossing west = a.chunk().x() <= b.chunk().x() ? a : b;
            DetectedRoadCrossing east = west == a ? b : a;
            return west.approaches().contains(ApproachDirection.EAST)
                    && east.approaches().contains(ApproachDirection.WEST);
        }
        DetectedRoadCrossing north = a.chunk().z() <= b.chunk().z() ? a : b;
        DetectedRoadCrossing south = north == a ? b : a;
        return north.approaches().contains(ApproachDirection.SOUTH)
                && south.approaches().contains(ApproachDirection.NORTH);
    }

    private static boolean compatibleSharedGrade(
            PlannedInterchange left,
            PlannedInterchange right,
            RoadAxis axis) {
        CrossingDecks leftDecks = left.crossing().decks();
        CrossingDecks rightDecks = right.crossing().decks();
        if (axis == RoadAxis.X) {
            return leftDecks.nativeX().equals(leftDecks.plannedX())
                    && rightDecks.nativeX().equals(rightDecks.plannedX())
                    && leftDecks.nativeX().equals(rightDecks.nativeX());
        }
        return leftDecks.nativeZ().equals(leftDecks.plannedZ())
                && rightDecks.nativeZ().equals(rightDecks.plannedZ())
                && leftDecks.nativeZ().equals(rightDecks.nativeZ());
    }

    private static boolean anyEnvelopeComponentOverlaps(
            CompiledEnvelope left,
            CompiledEnvelope right) {
        List<Rectangle> leftComponents = List.of(
                left.core(), left.xCorridor(), left.zCorridor());
        List<Rectangle> rightComponents = List.of(
                right.core(), right.xCorridor(), right.zCorridor());
        return leftComponents.stream().anyMatch(leftComponent ->
                rightComponents.stream().anyMatch(leftComponent::overlaps));
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
        return left.crossing().compareTo(right.crossing());
    }

    private static int ceilDiv(int dividend, int divisor) {
        return dividend == 0 ? 0 : 1 + (dividend - 1) / divisor;
    }

    private record CompiledEnvelope(
            long centerX,
            long centerZ,
            Rectangle core,
            Rectangle xCorridor,
            Rectangle zCorridor) {
    }

    private record Rectangle(long minX, long maxX, long minZ, long maxZ) {
        private Rectangle {
            if (minX > maxX || minZ > maxZ) {
                throw new IllegalArgumentException("Compiled envelope is inverted");
            }
        }

        private static Rectangle around(
                long centerX,
                long centerZ,
                int radiusX,
                int radiusZ) {
            return new Rectangle(
                    centerX - radiusX,
                    centerX + radiusX,
                    centerZ - radiusZ,
                    centerZ + radiusZ);
        }

        private boolean overlaps(Rectangle other) {
            return minX < other.maxX && maxX > other.minX
                    && minZ < other.maxZ && maxZ > other.minZ;
        }
    }
}
