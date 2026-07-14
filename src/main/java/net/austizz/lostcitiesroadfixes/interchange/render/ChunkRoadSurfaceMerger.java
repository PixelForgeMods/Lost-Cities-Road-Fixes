package net.austizz.lostcitiesroadfixes.interchange.render;

import net.austizz.lostcitiesroadfixes.render.ChunkRoadSurface;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceCell;
import net.austizz.lostcitiesroadfixes.render.RoadSurfacePosition;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRole;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ChunkRoadSurfaceMerger {
    public ChunkRoadSurface merge(
            ChunkPoint targetChunk,
            Collection<ChunkRoadSurface> surfaces) {
        Objects.requireNonNull(targetChunk, "targetChunk");
        Objects.requireNonNull(surfaces, "surfaces");
        Map<RoadSurfacePosition, RoadSurfaceRole> merged = new HashMap<>();
        for (ChunkRoadSurface surface : surfaces) {
            if (!surface.chunk().equals(targetChunk)) {
                throw new IllegalArgumentException("Cannot merge a surface from another chunk");
            }
            for (RoadSurfaceCell cell : surface.cells()) {
                merged.merge(cell.position(), cell.role(), ChunkRoadSurfaceMerger::higherPriority);
            }
        }
        List<RoadSurfaceCell> cells = new ArrayList<>(merged.size());
        merged.forEach((position, role) -> cells.add(new RoadSurfaceCell(position, role)));
        return new ChunkRoadSurface(targetChunk, cells);
    }

    private static RoadSurfaceRole higherPriority(RoadSurfaceRole left, RoadSurfaceRole right) {
        return priority(left) >= priority(right) ? left : right;
    }

    private static int priority(RoadSurfaceRole role) {
        return switch (role) {
            case AT_GRADE_INTERSECTION -> 6;
            case ASPHALT -> 5;
            case WHITE_MARKING, YELLOW_MARKING -> 4;
            case SHOULDER -> 3;
            case MEDIAN -> 2;
        };
    }
}
