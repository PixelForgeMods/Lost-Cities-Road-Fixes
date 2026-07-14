package net.austizz.lostcitiesroadfixes.planning.continuity;

import java.util.Comparator;
import java.util.Objects;

public record RoadTile(RoadTileKey key, int level, RoadTileOrigin origin) {
    public static final Comparator<RoadTile> ORDER = Comparator.comparing(RoadTile::key);

    public RoadTile {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(origin, "origin");
        if (level < 0) {
            throw new IllegalArgumentException("Road level cannot be negative");
        }
    }
}
