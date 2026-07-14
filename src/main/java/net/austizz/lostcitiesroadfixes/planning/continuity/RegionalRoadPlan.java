package net.austizz.lostcitiesroadfixes.planning.continuity;

import net.austizz.lostcitiesroadfixes.planning.RoadPlanKey;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RegionalRoadPlan {
    private final RoadPlanKey key;
    private final List<RoadTile> tiles;
    private final Map<RoadTileKey, RoadTile> byKey;

    public RegionalRoadPlan(RoadPlanKey key, List<RoadTile> tiles) {
        this.key = Objects.requireNonNull(key, "key");
        List<RoadTile> sorted = Objects.requireNonNull(tiles, "tiles").stream()
                .sorted(RoadTile.ORDER)
                .toList();
        Map<RoadTileKey, RoadTile> index = new HashMap<>();
        for (RoadTile tile : sorted) {
            if (!key.region().owns(tile.key().chunk())) {
                throw new IllegalArgumentException(
                        "Road plan contains a tile outside owning region: " + tile.key());
            }
            if (index.put(tile.key(), tile) != null) {
                throw new IllegalArgumentException("Road plan contains duplicate tile " + tile.key());
            }
        }
        this.tiles = sorted;
        this.byKey = Map.copyOf(index);
    }

    public RoadPlanKey key() {
        return key;
    }

    public List<RoadTile> tiles() {
        return tiles;
    }

    public Optional<RoadTile> tileAt(ChunkPoint chunk, RoadAxis axis) {
        return Optional.ofNullable(byKey.get(new RoadTileKey(chunk, axis)));
    }
}
