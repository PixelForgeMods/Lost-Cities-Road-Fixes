package net.austizz.lostcitiesroadfixes.render;

import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;

import java.util.Objects;

public record RoadSurfacePosition(int x, int z, HalfBlockElevation elevation)
        implements Comparable<RoadSurfacePosition> {

    public RoadSurfacePosition {
        Objects.requireNonNull(elevation, "elevation");
    }

    @Override
    public int compareTo(RoadSurfacePosition other) {
        int byElevation = elevation.compareTo(other.elevation);
        if (byElevation != 0) {
            return byElevation;
        }
        int byZ = Integer.compare(z, other.z);
        return byZ != 0 ? byZ : Integer.compare(x, other.x);
    }
}
