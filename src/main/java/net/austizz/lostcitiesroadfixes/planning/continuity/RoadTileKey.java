package net.austizz.lostcitiesroadfixes.planning.continuity;

import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.Objects;

public record RoadTileKey(ChunkPoint chunk, RoadAxis axis) implements Comparable<RoadTileKey> {
    public RoadTileKey {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(axis, "axis");
    }

    @Override
    public int compareTo(RoadTileKey other) {
        int byZ = Integer.compare(chunk.z(), other.chunk.z());
        if (byZ != 0) {
            return byZ;
        }
        int byX = Integer.compare(chunk.x(), other.chunk.x());
        if (byX != 0) {
            return byX;
        }
        return axis.compareTo(other.axis);
    }
}
