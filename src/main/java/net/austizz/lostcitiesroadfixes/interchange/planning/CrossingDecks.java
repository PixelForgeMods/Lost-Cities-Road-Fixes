package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;

import java.util.Objects;

public record CrossingDecks(
        HalfBlockElevation nativeX,
        HalfBlockElevation nativeZ,
        HalfBlockElevation plannedX,
        HalfBlockElevation plannedZ) {
    public CrossingDecks {
        Objects.requireNonNull(nativeX, "nativeX");
        Objects.requireNonNull(nativeZ, "nativeZ");
        Objects.requireNonNull(plannedX, "plannedX");
        Objects.requireNonNull(plannedZ, "plannedZ");
        if (nativeX.equals(nativeZ)) {
            throw new IllegalArgumentException("An interchange requires differing native deck elevations");
        }
        if (plannedX.equals(plannedZ)) {
            throw new IllegalArgumentException("Planned interchange decks cannot share an elevation");
        }
    }

    public HalfBlockElevation lowerPlannedDeck() {
        return plannedX.compareTo(plannedZ) < 0 ? plannedX : plannedZ;
    }

    public HalfBlockElevation upperPlannedDeck() {
        return plannedX.compareTo(plannedZ) > 0 ? plannedX : plannedZ;
    }
}
