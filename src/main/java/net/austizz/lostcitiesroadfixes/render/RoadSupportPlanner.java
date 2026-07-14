package net.austizz.lostcitiesroadfixes.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Selects up to two spread support columns from the lowest road deck. */
public final class RoadSupportPlanner {
    public List<RoadSurfacePosition> anchors(ChunkRoadSurface surface) {
        Objects.requireNonNull(surface, "surface");
        Map<Column, RoadSurfacePosition> lowestByColumn = new HashMap<>();
        for (RoadSurfaceCell cell : surface.cells()) {
            RoadSurfacePosition position = cell.position();
            lowestByColumn.merge(
                    new Column(position.x(), position.z()),
                    position,
                    (left, right) -> left.elevation().compareTo(right.elevation()) <= 0
                            ? left
                            : right);
        }
        if (lowestByColumn.isEmpty()) {
            return List.of();
        }

        List<RoadSurfacePosition> candidates = List.copyOf(lowestByColumn.values());
        List<RoadSurfacePosition> result = new ArrayList<>(2);
        addNearest(result, candidates, surface.chunk().minBlockX(), surface.chunk().minBlockZ());
        addNearest(result, candidates, surface.chunk().maxBlockX(), surface.chunk().maxBlockZ());
        return List.copyOf(result);
    }

    private static void addNearest(
            List<RoadSurfacePosition> result,
            List<RoadSurfacePosition> candidates,
            int targetX,
            int targetZ) {
        candidates.stream()
                .filter(candidate -> !result.contains(candidate))
                .min(Comparator
                        .comparingLong((RoadSurfacePosition candidate) ->
                                squaredDistance(candidate, targetX, targetZ))
                        .thenComparing(RoadSurfacePosition::compareTo))
                .ifPresent(result::add);
    }

    private static long squaredDistance(
            RoadSurfacePosition position,
            int targetX,
            int targetZ) {
        long deltaX = (long) position.x() - targetX;
        long deltaZ = (long) position.z() - targetZ;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    private record Column(int x, int z) {
    }
}
