package net.austizz.lostcitiesroadfixes.integration;

import mcjty.lostcities.api.MultiPos;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.Objects;
import java.util.function.Predicate;

public final class RoadBuildingExclusionPolicy {
    public static final int CLEARANCE_CHUNKS = 1;

    private RoadBuildingExclusionPolicy() {
    }

    public static boolean intersectsReservedRoadArea(
            ChunkPoint currentChunk,
            MultiPos footprint,
            Predicate<ChunkPoint> roadSurfacePresent) {
        Objects.requireNonNull(currentChunk, "currentChunk");
        Objects.requireNonNull(footprint, "footprint");
        Objects.requireNonNull(roadSurfacePresent, "roadSurfacePresent");
        int localX = footprint.isSingle() ? 0 : footprint.x();
        int localZ = footprint.isSingle() ? 0 : footprint.z();
        int widthChunks = footprint.w();
        int depthChunks = footprint.h();
        if (localX < 0 || localZ < 0 || widthChunks < 1 || depthChunks < 1
                || localX >= widthChunks || localZ >= depthChunks) {
            throw new IllegalArgumentException("Invalid multi-building footprint");
        }
        int minimumX = Math.subtractExact(currentChunk.x(), localX);
        int minimumZ = Math.subtractExact(currentChunk.z(), localZ);
        int maximumX = Math.addExact(minimumX, widthChunks - 1);
        int maximumZ = Math.addExact(minimumZ, depthChunks - 1);
        for (int z = minimumZ - CLEARANCE_CHUNKS;
             z <= maximumZ + CLEARANCE_CHUNKS; z++) {
            for (int x = minimumX - CLEARANCE_CHUNKS;
                 x <= maximumX + CLEARANCE_CHUNKS; x++) {
                if (roadSurfacePresent.test(new ChunkPoint(x, z))) {
                    return true;
                }
            }
        }
        return false;
    }
}
