package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDecision;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.Objects;

public record ConflictedRoadCrossing(
        DetectedRoadCrossing crossing,
        InterchangeDecision decision,
        ChunkPoint blockingCrossing,
        int minimumCenterSeparationBlocks) implements Comparable<ConflictedRoadCrossing> {
    public ConflictedRoadCrossing {
        Objects.requireNonNull(crossing, "crossing");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(blockingCrossing, "blockingCrossing");
        if (decision.selected().isEmpty()) {
            throw new IllegalArgumentException(
                    "A conflicted crossing requires a feasible interchange design");
        }
        if (!decision.site().equals(crossing.selectionSite())) {
            throw new IllegalArgumentException("Interchange decision does not belong to crossing");
        }
        if (blockingCrossing.equals(crossing.chunk())) {
            throw new IllegalArgumentException("A crossing cannot block itself");
        }
        if (minimumCenterSeparationBlocks < 1) {
            throw new IllegalArgumentException("Conflict separation must be positive");
        }
    }

    public String diagnostic() {
        return "interchange core overlaps higher-priority crossing at chunk "
                + blockingCrossing.x() + "," + blockingCrossing.z()
                + "; centers need at least " + minimumCenterSeparationBlocks
                + " blocks of separation";
    }

    @Override
    public int compareTo(ConflictedRoadCrossing other) {
        return crossing.compareTo(other.crossing);
    }
}
