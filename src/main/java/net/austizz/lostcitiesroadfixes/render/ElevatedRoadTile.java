package net.austizz.lostcitiesroadfixes.render;

import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;

import java.util.Comparator;
import java.util.Objects;

public record ElevatedRoadTile(ChunkPoint chunk, RoadAxis axis, HalfBlockElevation elevation) {
    public static final Comparator<ElevatedRoadTile> ORDER = Comparator
            .comparing((ElevatedRoadTile tile) -> tile.chunk().z())
            .thenComparing(tile -> tile.chunk().x())
            .thenComparing(ElevatedRoadTile::axis)
            .thenComparing(ElevatedRoadTile::elevation);

    public ElevatedRoadTile {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(axis, "axis");
        Objects.requireNonNull(elevation, "elevation");
    }
}
