package net.austizz.lostcitiesroadfixes.planning.continuity;

import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.OptionalInt;

@FunctionalInterface
public interface RoadObservationSource {
    OptionalInt roadLevel(ChunkPoint chunk, RoadAxis axis);
}
