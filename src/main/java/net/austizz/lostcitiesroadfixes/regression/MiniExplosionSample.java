package net.austizz.lostcitiesroadfixes.regression;

import net.austizz.lostcitiesroadfixes.road.BlockPoint;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

public record MiniExplosionSample(float chanceRoll, int radius, BlockPoint center) {
    private static final int LOST_CITIES_MIN_Y = 0;
    private static final int LOST_CITIES_MAX_Y = 256;

    public MiniExplosionSample {
        if (radius < 1) {
            throw new IllegalArgumentException("Explosion radius must be positive");
        }
    }

    public boolean intersects(ChunkPoint chunk) {
        long dx = distanceToRange(center.x(), chunk.minBlockX(), chunk.maxBlockX());
        long dy = distanceToRange(center.y(), LOST_CITIES_MIN_Y, LOST_CITIES_MAX_Y);
        long dz = distanceToRange(center.z(), chunk.minBlockZ(), chunk.maxBlockZ());
        return dx * dx + dy * dy + dz * dz <= (long) radius * radius;
    }

    private static long distanceToRange(int value, int minimum, int maximum) {
        if (value < minimum) {
            return (long) minimum - value;
        }
        if (value > maximum) {
            return (long) value - maximum;
        }
        return 0;
    }
}
