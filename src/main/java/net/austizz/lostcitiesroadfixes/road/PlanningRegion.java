package net.austizz.lostcitiesroadfixes.road;

public record PlanningRegion(int x, int z) {
    public int ownershipMinChunkX() {
        return Math.multiplyExact(x, PlanningGrid.REGION_SIZE_CHUNKS);
    }

    public int ownershipMaxChunkX() {
        return Math.addExact(ownershipMinChunkX(), PlanningGrid.REGION_SIZE_CHUNKS - 1);
    }

    public int ownershipMinChunkZ() {
        return Math.multiplyExact(z, PlanningGrid.REGION_SIZE_CHUNKS);
    }

    public int ownershipMaxChunkZ() {
        return Math.addExact(ownershipMinChunkZ(), PlanningGrid.REGION_SIZE_CHUNKS - 1);
    }

    public int planningMinChunkX() {
        return Math.subtractExact(ownershipMinChunkX(), PlanningGrid.HALO_CHUNKS);
    }

    public int planningMaxChunkX() {
        return Math.addExact(ownershipMaxChunkX(), PlanningGrid.HALO_CHUNKS);
    }

    public int planningMinChunkZ() {
        return Math.subtractExact(ownershipMinChunkZ(), PlanningGrid.HALO_CHUNKS);
    }

    public int planningMaxChunkZ() {
        return Math.addExact(ownershipMaxChunkZ(), PlanningGrid.HALO_CHUNKS);
    }

    public boolean owns(ChunkPoint chunk) {
        return chunk.x() >= ownershipMinChunkX()
                && chunk.x() <= ownershipMaxChunkX()
                && chunk.z() >= ownershipMinChunkZ()
                && chunk.z() <= ownershipMaxChunkZ();
    }

    public boolean isInsidePlanningHalo(ChunkPoint chunk) {
        return chunk.x() >= planningMinChunkX()
                && chunk.x() <= planningMaxChunkX()
                && chunk.z() >= planningMinChunkZ()
                && chunk.z() <= planningMaxChunkZ();
    }
}
