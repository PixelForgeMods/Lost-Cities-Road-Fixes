package net.austizz.lostcitiesroadfixes.render;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Last-line validation for the composed road surface. Joined cells may share
 * an elevation, but distinct decks in one block column must leave the full
 * vehicle envelope between them.
 */
public final class RoadSurfaceClearanceValidator {
    private final int minimumSeparationHalfBlocks;

    public RoadSurfaceClearanceValidator(int minimumSeparationBlocks) {
        if (minimumSeparationBlocks < 1) {
            throw new IllegalArgumentException("Minimum separation must be positive");
        }
        minimumSeparationHalfBlocks = Math.multiplyExact(
                minimumSeparationBlocks, 2);
    }

    public void requireSafe(ChunkRoadSurface surface) {
        Objects.requireNonNull(surface, "surface");
        Map<Column, TreeSet<Integer>> elevationsByColumn = new TreeMap<>();
        for (RoadSurfaceCell cell : surface.cells()) {
            RoadSurfacePosition position = cell.position();
            elevationsByColumn
                    .computeIfAbsent(
                            new Column(position.x(), position.z()),
                            ignored -> new TreeSet<>())
                    .add(position.elevation().halfBlocks());
        }

        for (Map.Entry<Column, TreeSet<Integer>> entry : elevationsByColumn.entrySet()) {
            Integer lower = null;
            for (int upper : entry.getValue()) {
                if (lower != null && upper - lower < minimumSeparationHalfBlocks) {
                    Column column = entry.getKey();
                    throw new IllegalArgumentException(
                            "Unsafe road decks at " + column.x() + "," + column.z()
                                    + " use elevations " + lower + " and " + upper
                                    + " half-blocks; minimum separation is "
                                    + minimumSeparationHalfBlocks + " half-blocks");
                }
                lower = upper;
            }
        }
    }

    private record Column(int x, int z) implements Comparable<Column> {
        @Override
        public int compareTo(Column other) {
            int byZ = Integer.compare(z, other.z);
            return byZ != 0 ? byZ : Integer.compare(x, other.x);
        }
    }
}
