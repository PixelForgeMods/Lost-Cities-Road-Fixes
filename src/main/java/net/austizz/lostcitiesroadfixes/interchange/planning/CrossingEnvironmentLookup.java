package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeEnvironment;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

@FunctionalInterface
public interface CrossingEnvironmentLookup {
    InterchangeEnvironment survey(ChunkPoint center, int radiusBlocks);

    static CrossingEnvironmentLookup empty() {
        return (center, radiusBlocks) -> InterchangeEnvironment.empty(center);
    }
}
