package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadTile;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.Optional;

@FunctionalInterface
public interface RoadTileLookup {
    Optional<RoadTile> tileAt(ChunkPoint chunk, RoadAxis axis);
}
