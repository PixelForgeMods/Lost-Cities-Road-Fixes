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
        long selectionSeed,
        InterchangeEnvironment environment,
        HalfBlockElevation xRoadNativeElevation,
        HalfBlockElevation zRoadNativeElevation,
        HalfBlockElevation xRoadCenterElevation,
        HalfBlockElevation zRoadCenterElevation) {
    public InterchangeSite {
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(lowerDeck, "lowerDeck");
        Objects.requireNonNull(upperDeck, "upperDeck");
        Objects.requireNonNull(demand, "demand");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(xRoadNativeElevation, "xRoadNativeElevation");
        Objects.requireNonNull(zRoadNativeElevation, "zRoadNativeElevation");
        Objects.requireNonNull(xRoadCenterElevation, "xRoadCenterElevation");
        Objects.requireNonNull(zRoadCenterElevation, "zRoadCenterElevation");
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

    public InterchangeSite(
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
        this(
                form,
                availableRadiusBlocks,
                availableQuadrants,
                approachRunBlocks,
                lowerDeck,
                upperDeck,
                demand,
                maximumStructureLevels,
                loopRampsAllowed,
                requireAllMovementsFreeFlow,
                selectionSeed,
                InterchangeEnvironment.empty(new net.austizz.lostcitiesroadfixes.road.ChunkPoint(
                        0, 0)),
                lowerDeck,
                upperDeck,
                lowerDeck,
                upperDeck);
    }
}
