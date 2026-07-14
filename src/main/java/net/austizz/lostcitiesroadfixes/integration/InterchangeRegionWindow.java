package net.austizz.lostcitiesroadfixes.integration;

import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.PlanningGrid;
import net.austizz.lostcitiesroadfixes.road.PlanningRegion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InterchangeRegionWindow {
    private static final int CHUNK_WIDTH_BLOCKS = 16;

    private InterchangeRegionWindow() {
    }

    public static List<PlanningRegion> ownerRegionsAffecting(
            ChunkPoint target,
            int maximumInfluenceBlocks) {
        Objects.requireNonNull(target, "target");
        if (maximumInfluenceBlocks < 0) {
            throw new IllegalArgumentException("Maximum influence cannot be negative");
        }
        int chunkRadius = Math.floorDiv(
                Math.addExact(maximumInfluenceBlocks, CHUNK_WIDTH_BLOCKS - 1),
                CHUNK_WIDTH_BLOCKS);
        PlanningRegion minimum = PlanningGrid.regionFor(new ChunkPoint(
                Math.subtractExact(target.x(), chunkRadius),
                Math.subtractExact(target.z(), chunkRadius)));
        PlanningRegion maximum = PlanningGrid.regionFor(new ChunkPoint(
                Math.addExact(target.x(), chunkRadius),
                Math.addExact(target.z(), chunkRadius)));
        List<PlanningRegion> result = new ArrayList<>();
        for (int regionZ = minimum.z(); regionZ <= maximum.z(); regionZ++) {
            for (int regionX = minimum.x(); regionX <= maximum.x(); regionX++) {
                result.add(new PlanningRegion(regionX, regionZ));
            }
        }
        return List.copyOf(result);
    }
}
