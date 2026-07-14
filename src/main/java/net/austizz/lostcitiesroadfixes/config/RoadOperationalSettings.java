package net.austizz.lostcitiesroadfixes.config;

import net.austizz.lostcitiesroadfixes.theme.RoadThemeId;

import java.util.Objects;

/** Immutable safe settings captured once for a planning/render operation. */
public record RoadOperationalSettings(
        int maximumGapChunks,
        int maximumCachedRegions,
        RoadThemeId activeThemeId,
        boolean logFirstInterchangeSelection) {
    public static final int MINIMUM_GAP_CHUNKS = 1;
    public static final int MAXIMUM_GAP_CHUNKS = 4;
    public static final int MINIMUM_CACHED_REGIONS = 64;
    public static final int MAXIMUM_CACHED_REGIONS = 4_096;

    public RoadOperationalSettings {
        if (maximumGapChunks < MINIMUM_GAP_CHUNKS
                || maximumGapChunks > MAXIMUM_GAP_CHUNKS) {
            throw new IllegalArgumentException(
                    "Maximum gap chunks must be between " + MINIMUM_GAP_CHUNKS
                            + " and " + MAXIMUM_GAP_CHUNKS);
        }
        if (maximumCachedRegions < MINIMUM_CACHED_REGIONS
                || maximumCachedRegions > MAXIMUM_CACHED_REGIONS) {
            throw new IllegalArgumentException(
                    "Maximum cached regions must be between " + MINIMUM_CACHED_REGIONS
                            + " and " + MAXIMUM_CACHED_REGIONS);
        }
        Objects.requireNonNull(activeThemeId, "activeThemeId");
    }

    public String planningFingerprint() {
        return "road-operational-settings-v1|maximum-gap-chunks=" + maximumGapChunks;
    }
}
