package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;

import java.util.Objects;

public record InterchangeSite(
        JunctionForm form,
        int availableRadiusBlocks,
        int availableQuadrants,
        int approachRunBlocks,
        HalfBlockElevation lowerDeck,
        HalfBlockElevation upperDeck,
        TrafficDemand demand,
        int maximumStructureLevels,
        boolean loopRampsAllowed,
        boolean requireAllMovementsFreeFlow,
        long selectionSeed) {
    public InterchangeSite {
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(lowerDeck, "lowerDeck");
        Objects.requireNonNull(upperDeck, "upperDeck");
        Objects.requireNonNull(demand, "demand");
        if (availableRadiusBlocks < 1 || approachRunBlocks < 0) {
            throw new IllegalArgumentException("Site dimensions cannot be negative or zero");
        }
        if (availableQuadrants < 1 || availableQuadrants > 4) {
            throw new IllegalArgumentException("Available quadrants must be between one and four");
        }
        if (maximumStructureLevels < 1) {
            throw new IllegalArgumentException("A site must support at least one structure level");
        }
        if (lowerDeck.compareTo(upperDeck) >= 0) {
            throw new IllegalArgumentException("The lower deck must be below the upper deck");
        }
    }
}
