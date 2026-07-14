package net.austizz.lostcitiesroadfixes.diagnostics;

import java.util.List;
import java.util.Objects;

public record RoadDiagnosticsSnapshot(
        boolean compatible,
        long nativeSuppressions,
        long lateRenderInvocations,
        long interchangeRegionsPlanned,
        long selectedInterchanges,
        long rejectedCrossings,
        long interchangeRenderInvocations,
        int roadCacheSize,
        int interchangeCacheSize,
        int loadedDesigns,
        int loadedThemes,
        String configuredThemeId,
        String resolvedThemeId,
        int maximumGapChunks,
        int maximumCachedRegions,
        boolean logFirstInterchangeSelection) {
    public RoadDiagnosticsSnapshot {
        requireNonNegative(nativeSuppressions, "nativeSuppressions");
        requireNonNegative(lateRenderInvocations, "lateRenderInvocations");
        requireNonNegative(interchangeRegionsPlanned, "interchangeRegionsPlanned");
        requireNonNegative(selectedInterchanges, "selectedInterchanges");
        requireNonNegative(rejectedCrossings, "rejectedCrossings");
        requireNonNegative(interchangeRenderInvocations, "interchangeRenderInvocations");
        requireNonNegative(roadCacheSize, "roadCacheSize");
        requireNonNegative(interchangeCacheSize, "interchangeCacheSize");
        requireNonNegative(loadedDesigns, "loadedDesigns");
        requireNonNegative(loadedThemes, "loadedThemes");
        configuredThemeId = requireText(configuredThemeId, "configuredThemeId");
        resolvedThemeId = requireText(resolvedThemeId, "resolvedThemeId");
        if (maximumGapChunks < 1 || maximumCachedRegions < 1) {
            throw new IllegalArgumentException("Diagnostic bounds must be positive");
        }
    }

    public List<String> lines() {
        boolean fallback = !configuredThemeId.equals(resolvedThemeId);
        return List.of(
                "Lost Cities: Road Fixes diagnostics",
                "compatibility: compatible=" + compatible,
                "hooks: nativeSuppressions=" + nativeSuppressions
                        + ", lateRenders=" + lateRenderInvocations,
                "interchanges: regions=" + interchangeRegionsPlanned
                        + ", selected=" + selectedInterchanges
                        + ", rejected=" + rejectedCrossings
                        + ", renderedChunks=" + interchangeRenderInvocations,
                "caches: roads=" + roadCacheSize
                        + ", interchanges=" + interchangeCacheSize,
                "resources: loadedDesigns=" + loadedDesigns
                        + ", loadedThemes=" + loadedThemes,
                "theme: configured=" + configuredThemeId
                        + ", resolved=" + resolvedThemeId
                        + ", fallback=" + fallback,
                "settings: maximumGapChunks=" + maximumGapChunks
                        + ", maximumCachedRegions=" + maximumCachedRegions
                        + ", logFirstInterchangeSelection=" + logFirstInterchangeSelection);
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }
}
