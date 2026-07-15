package net.austizz.lostcitiesroadfixes.render;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Plans a taller arched vehicle envelope from the rendered road shoulders.
 * Terrain above the shoulder remains lower than terrain above the travel lanes,
 * producing a rounded tunnel crown instead of a rectangular slot.
 */
public final class RoadClearanceEnvelopePlanner {
    public static final int SHOULDER_HEADROOM_BLOCKS = 8;
    public static final int MAXIMUM_HEADROOM_BLOCKS = 12;

    public Map<RoadSurfacePosition, Integer> plan(ChunkRoadSurface surface) {
        Objects.requireNonNull(surface, "surface");
        List<RoadSurfacePosition> shoulders = surface.cells().stream()
                .filter(cell -> cell.role() == RoadSurfaceRole.SHOULDER)
                .map(RoadSurfaceCell::position)
                .toList();
        Map<RoadSurfacePosition, Integer> result = new HashMap<>();
        for (RoadSurfaceCell cell : surface.cells()) {
            result.put(cell.position(), headroom(cell, shoulders));
        }
        return Map.copyOf(result);
    }

    private static int headroom(
            RoadSurfaceCell cell,
            List<RoadSurfacePosition> shoulders) {
        if (cell.role() == RoadSurfaceRole.SHOULDER) {
            return SHOULDER_HEADROOM_BLOCKS;
        }
        double nearestShoulderDistanceSquared = Double.POSITIVE_INFINITY;
        for (RoadSurfacePosition shoulder : shoulders) {
            if (!shoulder.elevation().equals(cell.position().elevation())) {
                continue;
            }
            long deltaX = (long) shoulder.x() - cell.position().x();
            long deltaZ = (long) shoulder.z() - cell.position().z();
            double distanceSquared = (double) deltaX * deltaX + (double) deltaZ * deltaZ;
            nearestShoulderDistanceSquared = StrictMath.min(
                    nearestShoulderDistanceSquared, distanceSquared);
        }
        if (!Double.isFinite(nearestShoulderDistanceSquared)) {
            return MAXIMUM_HEADROOM_BLOCKS;
        }
        double distanceFromShoulder = StrictMath.sqrt(nearestShoulderDistanceSquared);
        int archRise = (int) StrictMath.ceil(3.0 * StrictMath.sqrt(distanceFromShoulder));
        return StrictMath.min(
                MAXIMUM_HEADROOM_BLOCKS,
                SHOULDER_HEADROOM_BLOCKS + archRise);
    }
}
