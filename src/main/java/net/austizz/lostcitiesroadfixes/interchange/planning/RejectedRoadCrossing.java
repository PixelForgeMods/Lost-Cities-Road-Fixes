package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDecision;

import java.util.Objects;

public record RejectedRoadCrossing(
        DetectedRoadCrossing crossing,
        InterchangeDecision decision) implements Comparable<RejectedRoadCrossing> {
    public RejectedRoadCrossing {
        Objects.requireNonNull(crossing, "crossing");
        Objects.requireNonNull(decision, "decision");
        if (decision.selected().isPresent()) {
            throw new IllegalArgumentException("A rejected crossing cannot have a selected design");
        }
        if (!decision.site().equals(crossing.selectionSite())) {
            throw new IllegalArgumentException("Interchange decision does not belong to crossing");
        }
    }

    @Override
    public int compareTo(RejectedRoadCrossing other) {
        return crossing.compareTo(other.crossing);
    }
}
