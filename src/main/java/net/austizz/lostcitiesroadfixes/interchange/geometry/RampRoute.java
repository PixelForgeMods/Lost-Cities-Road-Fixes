package net.austizz.lostcitiesroadfixes.interchange.geometry;

import java.util.Objects;

public record RampRoute(RampCenterline centerline, int widthBlocks) {
    public RampRoute {
        Objects.requireNonNull(centerline, "centerline");
        if (widthBlocks < 3 || widthBlocks > 16) {
            throw new IllegalArgumentException("Ramp width must be between 3 and 16 blocks");
        }
    }
}
