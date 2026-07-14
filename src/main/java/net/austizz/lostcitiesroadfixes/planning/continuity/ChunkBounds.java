package net.austizz.lostcitiesroadfixes.planning.continuity;

import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.PlanningRegion;

public record ChunkBounds(int minX, int maxX, int minZ, int maxZ) {
    public ChunkBounds {
        if (maxX < minX || maxZ < minZ) {
            throw new IllegalArgumentException("Chunk bounds must have non-empty X and Z ranges");
        }
    }

    public static ChunkBounds planningWindow(PlanningRegion region) {
        return new ChunkBounds(
                region.planningMinChunkX(),
                region.planningMaxChunkX(),
                region.planningMinChunkZ(),
                region.planningMaxChunkZ());
    }

    public boolean contains(ChunkPoint chunk) {
        return chunk.x() >= minX && chunk.x() <= maxX
                && chunk.z() >= minZ && chunk.z() <= maxZ;
    }
}
