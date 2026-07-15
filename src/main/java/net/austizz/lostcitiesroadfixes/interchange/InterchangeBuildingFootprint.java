package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.Objects;

public record InterchangeBuildingFootprint(
        int minChunkX,
        int maxChunkX,
        int minChunkZ,
        int maxChunkZ,
        int cityLevel) implements Comparable<InterchangeBuildingFootprint> {
    public InterchangeBuildingFootprint {
        if (minChunkX > maxChunkX || minChunkZ > maxChunkZ) {
            throw new IllegalArgumentException("Building footprint bounds are inverted");
        }
    }

    public boolean contains(ChunkPoint chunk) {
        Objects.requireNonNull(chunk, "chunk");
        return chunk.x() >= minChunkX && chunk.x() <= maxChunkX
                && chunk.z() >= minChunkZ && chunk.z() <= maxChunkZ;
    }

    public boolean intersectsSquare(ChunkPoint center, int radiusBlocks) {
        Objects.requireNonNull(center, "center");
        if (radiusBlocks < 0) {
            throw new IllegalArgumentException("Footprint radius cannot be negative");
        }
        long centerX = Math.addExact(Math.multiplyExact((long) center.x(), 16L), 8L);
        long centerZ = Math.addExact(Math.multiplyExact((long) center.z(), 16L), 8L);
        int minX = Math.toIntExact(Math.floorDiv(centerX - radiusBlocks, 16L));
        int maxX = Math.toIntExact(Math.floorDiv(centerX + radiusBlocks, 16L));
        int minZ = Math.toIntExact(Math.floorDiv(centerZ - radiusBlocks, 16L));
        int maxZ = Math.toIntExact(Math.floorDiv(centerZ + radiusBlocks, 16L));
        return maxChunkX >= minX && minChunkX <= maxX
                && maxChunkZ >= minZ && minChunkZ <= maxZ;
    }

    public int chunkCount() {
        return Math.multiplyExact(
                Math.addExact(Math.subtractExact(maxChunkX, minChunkX), 1),
                Math.addExact(Math.subtractExact(maxChunkZ, minChunkZ), 1));
    }

    @Override
    public int compareTo(InterchangeBuildingFootprint other) {
        int byZ = Integer.compare(minChunkZ, other.minChunkZ);
        if (byZ != 0) {
            return byZ;
        }
        int byX = Integer.compare(minChunkX, other.minChunkX);
        if (byX != 0) {
            return byX;
        }
        int byMaxZ = Integer.compare(maxChunkZ, other.maxChunkZ);
        return byMaxZ != 0 ? byMaxZ : Integer.compare(maxChunkX, other.maxChunkX);
    }
}
