package net.austizz.lostcitiesroadfixes.road;

public final class PlanningGrid {
    public static final int REGION_SIZE_CHUNKS = 64;
    public static final int HALO_CHUNKS = 32;

    private PlanningGrid() {
    }

    public static PlanningRegion regionFor(ChunkPoint chunk) {
        return new PlanningRegion(
                Math.floorDiv(chunk.x(), REGION_SIZE_CHUNKS),
                Math.floorDiv(chunk.z(), REGION_SIZE_CHUNKS));
    }
}
