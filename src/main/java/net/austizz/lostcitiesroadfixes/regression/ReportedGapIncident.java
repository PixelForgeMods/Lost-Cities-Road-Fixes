package net.austizz.lostcitiesroadfixes.regression;

import java.util.Objects;

public record ReportedGapIncident(
        long seed,
        String dimension,
        ChunkCoordinate explosionSourceChunk,
        ChunkCoordinate gapChunk,
        int cityLevel,
        GenerationPhase requiredRepairPhase) {

    public static final ReportedGapIncident JULY_2026 = new ReportedGapIncident(
            -6_377_442_428_365_110_436L,
            "minecraft:overworld",
            new ChunkCoordinate(-64, -140),
            new ChunkCoordinate(-64, -139),
            0,
            GenerationPhase.AFTER_LOST_CITIES_CLEANUP);

    public ReportedGapIncident {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(explosionSourceChunk, "explosionSourceChunk");
        Objects.requireNonNull(gapChunk, "gapChunk");
        Objects.requireNonNull(requiredRepairPhase, "requiredRepairPhase");
    }
}
