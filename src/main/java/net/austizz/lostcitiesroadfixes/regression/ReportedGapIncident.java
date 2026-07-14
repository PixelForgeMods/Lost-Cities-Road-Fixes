package net.austizz.lostcitiesroadfixes.regression;

import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.Objects;

public record ReportedGapIncident(
        long seed,
        String dimension,
        ChunkPoint explosionSourceChunk,
        ChunkPoint gapChunk,
        int cityLevel,
        GenerationPhase requiredRepairPhase) {

    public static final ReportedGapIncident JULY_2026 = new ReportedGapIncident(
            -6_377_442_428_365_110_436L,
            "minecraft:overworld",
            new ChunkPoint(-64, -140),
            new ChunkPoint(-64, -139),
            0,
            GenerationPhase.AFTER_LOST_CITIES_CLEANUP);

    public ReportedGapIncident {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(explosionSourceChunk, "explosionSourceChunk");
        Objects.requireNonNull(gapChunk, "gapChunk");
        Objects.requireNonNull(requiredRepairPhase, "requiredRepairPhase");
    }
}
