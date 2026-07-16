package net.austizz.lostcitiesroadfixes.diagnostics;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record RoadDiagnosticsSnapshot(
        boolean compatible,
        long nativeSuppressions,
        long buildingChunkSuppressions,
        long lateRenderInvocations,
        long interchangeRegionsPlanned,
        long selectedInterchanges,
        Map<InterchangeType, Long> selectedFamilies,
        long rejectedCrossings,
        long conflictedCrossings,
        long interchangeRenderInvocations,
        long straightThroughFallbacks,
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
        requireNonNegative(buildingChunkSuppressions, "buildingChunkSuppressions");
        requireNonNegative(lateRenderInvocations, "lateRenderInvocations");
        requireNonNegative(interchangeRegionsPlanned, "interchangeRegionsPlanned");
        requireNonNegative(selectedInterchanges, "selectedInterchanges");
        Objects.requireNonNull(selectedFamilies, "selectedFamilies");
        EnumMap<InterchangeType, Long> stableFamilies =
                new EnumMap<>(InterchangeType.class);
        selectedFamilies.forEach((type, count) -> {
            Objects.requireNonNull(type, "selected family type");
            Objects.requireNonNull(count, "selected family count");
            requireNonNegative(count, "selectedFamilies[" + type + "]");
            stableFamilies.put(type, count);
        });
        selectedFamilies = Map.copyOf(stableFamilies);
        requireNonNegative(rejectedCrossings, "rejectedCrossings");
        requireNonNegative(conflictedCrossings, "conflictedCrossings");
        requireNonNegative(interchangeRenderInvocations, "interchangeRenderInvocations");
        requireNonNegative(straightThroughFallbacks, "straightThroughFallbacks");
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
                        + ", buildingChunkSuppressions=" + buildingChunkSuppressions
                        + ", lateRenders=" + lateRenderInvocations,
                "interchanges: regions=" + interchangeRegionsPlanned
                        + ", selected=" + selectedInterchanges
                        + ", rejected=" + rejectedCrossings
                        + ", conflicted=" + conflictedCrossings
                        + ", renderedChunks=" + interchangeRenderInvocations
                        + ", straightThroughFallbacks=" + straightThroughFallbacks,
                "families: " + java.util.Arrays.stream(InterchangeType.values())
                        .map(type -> type.name().toLowerCase(java.util.Locale.ROOT)
                                + '=' + selectedFamilies.getOrDefault(type, 0L))
                        .collect(Collectors.joining(", ")),
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

    public String compactLine() {
        return String.join(" | ", lines());
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
