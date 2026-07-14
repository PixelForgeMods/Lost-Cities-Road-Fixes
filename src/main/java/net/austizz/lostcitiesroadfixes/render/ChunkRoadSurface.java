package net.austizz.lostcitiesroadfixes.render;

import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ChunkRoadSurface {
    private final ChunkPoint chunk;
    private final List<RoadSurfaceCell> cells;
    private final Map<RoadSurfacePosition, RoadSurfaceCell> byPosition;

    public ChunkRoadSurface(ChunkPoint chunk, List<RoadSurfaceCell> cells) {
        this.chunk = Objects.requireNonNull(chunk, "chunk");
        List<RoadSurfaceCell> sorted = Objects.requireNonNull(cells, "cells").stream()
                .sorted(RoadSurfaceCell.ORDER)
                .toList();
        Map<RoadSurfacePosition, RoadSurfaceCell> index = new HashMap<>();
        for (RoadSurfaceCell cell : sorted) {
            if (cell.position().x() < chunk.minBlockX() || cell.position().x() > chunk.maxBlockX()
                    || cell.position().z() < chunk.minBlockZ() || cell.position().z() > chunk.maxBlockZ()) {
                throw new IllegalArgumentException("Surface cell is outside requested chunk: " + cell.position());
            }
            if (index.put(cell.position(), cell) != null) {
                throw new IllegalArgumentException("Duplicate surface position " + cell.position());
            }
        }
        this.cells = sorted;
        this.byPosition = Map.copyOf(index);
    }

    public ChunkPoint chunk() {
        return chunk;
    }

    public List<RoadSurfaceCell> cells() {
        return cells;
    }

    public Optional<RoadSurfaceCell> cellAt(int x, int z, HalfBlockElevation elevation) {
        return Optional.ofNullable(byPosition.get(new RoadSurfacePosition(x, z, elevation)));
    }
}
