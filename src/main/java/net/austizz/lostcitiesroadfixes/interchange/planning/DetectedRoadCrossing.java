package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeSite;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.TrafficDemand;
import net.austizz.lostcitiesroadfixes.interchange.layout.ApproachDirection;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record DetectedRoadCrossing(
        ChunkPoint chunk,
        JunctionForm form,
        int xLevel,
        int zLevel,
        Set<ApproachDirection> approaches,
        int approachRunBlocks,
        int availableRadiusBlocks,
        int availableQuadrants,
        TrafficDemand demand,
        int maximumStructureLevels,
        boolean loopRampsAllowed,
        boolean requireAllMovementsFreeFlow,
        CrossingDecks decks,
        long selectionSeed) implements Comparable<DetectedRoadCrossing> {
    public DetectedRoadCrossing {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(approaches, "approaches");
        Objects.requireNonNull(demand, "demand");
        Objects.requireNonNull(decks, "decks");
        approaches = Collections.unmodifiableSet(EnumSet.copyOf(approaches));
        int expectedApproaches = form == JunctionForm.THREE_WAY ? 3 : 4;
        if (approaches.size() != expectedApproaches) {
            throw new IllegalArgumentException(
                    form + " crossing requires " + expectedApproaches + " approaches");
        }
        if (xLevel < 0 || zLevel < 0 || xLevel == zLevel) {
            throw new IllegalArgumentException("Crossing highway levels must be non-negative and different");
        }
        if (approachRunBlocks < 1 || availableRadiusBlocks < 1) {
            throw new IllegalArgumentException("Crossing dimensions must be positive");
        }
        if (availableQuadrants < 1 || availableQuadrants > 4) {
            throw new IllegalArgumentException("Available quadrants must be between one and four");
        }
        if (maximumStructureLevels < 1) {
            throw new IllegalArgumentException("Maximum structure levels must be positive");
        }
    }

    public InterchangeSite selectionSite() {
        return new InterchangeSite(
                form,
                availableRadiusBlocks,
                availableQuadrants,
                approachRunBlocks,
                decks.lowerPlannedDeck(),
                decks.upperPlannedDeck(),
                demand,
                maximumStructureLevels,
                loopRampsAllowed,
                requireAllMovementsFreeFlow,
                selectionSeed);
    }

    @Override
    public int compareTo(DetectedRoadCrossing other) {
        int byZ = Integer.compare(chunk.z(), other.chunk.z());
        return byZ != 0 ? byZ : Integer.compare(chunk.x(), other.chunk.x());
    }
}
