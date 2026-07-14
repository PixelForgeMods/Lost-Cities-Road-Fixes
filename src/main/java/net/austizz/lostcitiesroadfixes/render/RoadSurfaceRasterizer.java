package net.austizz.lostcitiesroadfixes.render;

import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RoadSurfaceRasterizer {
    private static final int CROSS_SECTION_WIDTH = 32;
    private static final int CROSS_SECTION_OFFSET = 8;
    private static final int DASH_PERIOD = 8;
    private static final int DASH_LENGTH = 4;

    public ChunkRoadSurface rasterize(ChunkPoint targetChunk, Collection<ElevatedRoadTile> roads) {
        Objects.requireNonNull(targetChunk, "targetChunk");
        Objects.requireNonNull(roads, "roads");

        Map<RoadSurfacePosition, Coverage> coverage = new HashMap<>();
        roads.stream().sorted(ElevatedRoadTile.ORDER).forEach(road ->
                addRoad(targetChunk, road, coverage));

        List<RoadSurfaceCell> cells = new ArrayList<>(coverage.size());
        coverage.forEach((position, value) ->
                cells.add(new RoadSurfaceCell(position, value.finalRole())));
        return new ChunkRoadSurface(targetChunk, cells);
    }

    private static void addRoad(
            ChunkPoint target,
            ElevatedRoadTile road,
            Map<RoadSurfacePosition, Coverage> coverage) {
        if (!canOverlap(target, road)) {
            return;
        }

        for (int z = target.minBlockZ(); z <= target.maxBlockZ(); z++) {
            for (int x = target.minBlockX(); x <= target.maxBlockX(); x++) {
                int crossIndex = crossIndex(road, x, z);
                if (crossIndex < 0 || crossIndex >= CROSS_SECTION_WIDTH) {
                    continue;
                }
                int longitudinal = road.axis() == RoadAxis.X ? x : z;
                RoadSurfaceRole role = arterialRoleAt(crossIndex, longitudinal);
                RoadSurfacePosition position = new RoadSurfacePosition(x, z, road.elevation());
                coverage.computeIfAbsent(position, ignored -> new Coverage()).add(road.axis(), role);
            }
        }
    }

    private static boolean canOverlap(ChunkPoint target, ElevatedRoadTile road) {
        if (road.axis() == RoadAxis.X) {
            return target.x() == road.chunk().x() && Math.abs(target.z() - road.chunk().z()) <= 1;
        }
        return target.z() == road.chunk().z() && Math.abs(target.x() - road.chunk().x()) <= 1;
    }

    private static int crossIndex(ElevatedRoadTile road, int x, int z) {
        int crossCoordinate = road.axis() == RoadAxis.X ? z : x;
        int roadCrossChunk = road.axis() == RoadAxis.X ? road.chunk().z() : road.chunk().x();
        return crossCoordinate - (roadCrossChunk * 16 - CROSS_SECTION_OFFSET);
    }

    public static RoadSurfaceRole arterialRoleAt(int crossIndex, int longitudinal) {
        if (crossIndex < 0 || crossIndex >= CROSS_SECTION_WIDTH) {
            throw new IllegalArgumentException("Arterial cross index must be between 0 and 31");
        }
        if (crossIndex == 0 || crossIndex == 31) {
            return RoadSurfaceRole.SHOULDER;
        }
        if (crossIndex == 15 || crossIndex == 16) {
            return RoadSurfaceRole.MEDIAN;
        }
        if (crossIndex == 14 || crossIndex == 17) {
            return RoadSurfaceRole.YELLOW_MARKING;
        }
        if ((crossIndex == 7 || crossIndex == 24)
                && Math.floorMod(longitudinal, DASH_PERIOD) < DASH_LENGTH) {
            return RoadSurfaceRole.WHITE_MARKING;
        }
        return RoadSurfaceRole.ASPHALT;
    }

    private static final class Coverage {
        private final EnumSet<RoadAxis> axes = EnumSet.noneOf(RoadAxis.class);
        private RoadSurfaceRole role;

        void add(RoadAxis axis, RoadSurfaceRole candidate) {
            axes.add(axis);
            if (role == null || rolePriority(candidate) > rolePriority(role)) {
                role = candidate;
            }
        }

        RoadSurfaceRole finalRole() {
            return axes.size() > 1 ? RoadSurfaceRole.AT_GRADE_INTERSECTION : role;
        }

        private static int rolePriority(RoadSurfaceRole role) {
            return switch (role) {
                case ASPHALT -> 5;
                case WHITE_MARKING, YELLOW_MARKING -> 4;
                case SHOULDER -> 3;
                case MEDIAN -> 2;
                case AT_GRADE_INTERSECTION -> 6;
            };
        }
    }
}
