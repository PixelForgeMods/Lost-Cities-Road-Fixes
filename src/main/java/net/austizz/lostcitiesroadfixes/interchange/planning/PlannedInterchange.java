package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDecision;

import java.util.Objects;

public record PlannedInterchange(
        DetectedRoadCrossing crossing,
        InterchangeDecision decision) implements Comparable<PlannedInterchange> {
    public PlannedInterchange {
        Objects.requireNonNull(crossing, "crossing");
        Objects.requireNonNull(decision, "decision");
        if (decision.selected().isEmpty()) {
            throw new IllegalArgumentException("A planned interchange requires a selected design");
        }
        if (!decision.site().equals(crossing.selectionSite())) {
            throw new IllegalArgumentException("Interchange decision does not belong to crossing");
        }
    }

    @Override
    public int compareTo(PlannedInterchange other) {
        return crossing.compareTo(other.crossing);
    }
}
