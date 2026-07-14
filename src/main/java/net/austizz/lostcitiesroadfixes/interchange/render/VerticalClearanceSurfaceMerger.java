package net.austizz.lostcitiesroadfixes.interchange.render;

import net.austizz.lostcitiesroadfixes.render.ChunkRoadSurface;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceCell;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Merges a ramp overlay onto an arterial surface while carving out deck cells
 * that would occupy the ramp's required vehicle-clearance envelope.
 */
public final class VerticalClearanceSurfaceMerger {
    private final int minimumSeparationHalfBlocks;
    private final ChunkRoadSurfaceMerger merger = new ChunkRoadSurfaceMerger();

    public VerticalClearanceSurfaceMerger(int minimumSeparationBlocks) {
        if (minimumSeparationBlocks < 1) {
            throw new IllegalArgumentException("Minimum separation must be positive");
        }
        minimumSeparationHalfBlocks = Math.multiplyExact(minimumSeparationBlocks, 2);
    }

    public ChunkRoadSurface merge(
            ChunkPoint targetChunk,
            ChunkRoadSurface arterialSurface,
            ChunkRoadSurface rampSurface) {
        Objects.requireNonNull(targetChunk, "targetChunk");
        Objects.requireNonNull(arterialSurface, "arterialSurface");
        Objects.requireNonNull(rampSurface, "rampSurface");
        if (!targetChunk.equals(arterialSurface.chunk())
                || !targetChunk.equals(rampSurface.chunk())) {
            throw new IllegalArgumentException("Every surface must belong to the target chunk");
        }

        Map<Column, List<Integer>> rampElevations = new HashMap<>();
        for (RoadSurfaceCell ramp : rampSurface.cells()) {
            Column column = new Column(ramp.position().x(), ramp.position().z());
            rampElevations.computeIfAbsent(column, ignored -> new java.util.ArrayList<>())
                    .add(ramp.position().elevation().halfBlocks());
        }
        ChunkRoadSurface carvedArterials = new ChunkRoadSurface(
                targetChunk,
                arterialSurface.cells().stream()
                        .filter(arterial -> !conflictsWithRamp(
                                arterial, rampElevations.get(new Column(
                                        arterial.position().x(), arterial.position().z()))))
                        .toList());
        return merger.merge(targetChunk, List.of(carvedArterials, rampSurface));
    }

    private boolean conflictsWithRamp(
            RoadSurfaceCell arterial,
            List<Integer> rampElevations) {
        if (rampElevations == null) {
            return false;
        }
        int arterialElevation = arterial.position().elevation().halfBlocks();
        return rampElevations.stream().anyMatch(rampElevation -> {
            int separation = StrictMath.abs(arterialElevation - rampElevation);
            return separation > 0 && separation <= minimumSeparationHalfBlocks;
        });
    }

    private record Column(int x, int z) {
    }
}
