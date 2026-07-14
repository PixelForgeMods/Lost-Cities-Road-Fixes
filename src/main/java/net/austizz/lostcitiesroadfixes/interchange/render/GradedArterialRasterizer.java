package net.austizz.lostcitiesroadfixes.interchange.render;

import net.austizz.lostcitiesroadfixes.render.ChunkRoadSurface;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceCell;
import net.austizz.lostcitiesroadfixes.render.RoadSurfacePosition;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRasterizer;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRole;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GradedArterialRasterizer {
    public ChunkRoadSurface rasterize(
            ChunkPoint targetChunk,
            Collection<GradedArterial> arterials) {
        Objects.requireNonNull(targetChunk, "targetChunk");
        Objects.requireNonNull(arterials, "arterials");
        Map<RoadSurfacePosition, RoadSurfaceRole> coverage = new HashMap<>();
        for (GradedArterial arterial : arterials) {
            addArterial(targetChunk, arterial, coverage);
        }
        List<RoadSurfaceCell> cells = new ArrayList<>(coverage.size());
        coverage.forEach((position, role) -> cells.add(new RoadSurfaceCell(position, role)));
        return new ChunkRoadSurface(targetChunk, cells);
    }

    private static void addArterial(
            ChunkPoint chunk,
            GradedArterial arterial,
            Map<RoadSurfacePosition, RoadSurfaceRole> coverage) {
        for (int z = chunk.minBlockZ(); z <= chunk.maxBlockZ(); z++) {
            for (int x = chunk.minBlockX(); x <= chunk.maxBlockX(); x++) {
                int longitudinal = arterial.axis() == net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis.X
                        ? x : z;
                if (!arterial.containsLongitudinal(longitudinal)) {
                    continue;
                }
                int crossCoordinate = arterial.axis() == net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis.X
                        ? z : x;
                int crossIndex = crossCoordinate - (arterial.centerCrossBlock() - 16);
                if (crossIndex < 0 || crossIndex >= 32) {
                    continue;
                }
                RoadSurfacePosition position = new RoadSurfacePosition(
                        x, z, arterial.elevationAt(longitudinal));
                RoadSurfaceRole role = RoadSurfaceRasterizer.arterialRoleAt(
                        crossIndex, longitudinal);
                coverage.merge(position, role, GradedArterialRasterizer::higherPriority);
            }
        }
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
