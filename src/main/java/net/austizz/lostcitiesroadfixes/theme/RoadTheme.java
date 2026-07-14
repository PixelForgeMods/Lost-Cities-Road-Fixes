package net.austizz.lostcitiesroadfixes.theme;

import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRole;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Minecraft-independent road-theme declaration loaded from a datapack. */
public record RoadTheme(
        RoadThemeId id,
        Map<RoadSurfaceRole, String> fullBlocks,
        Map<RoadSurfaceRole, String> bottomSlabs,
        String foundation,
        String support,
        int maximumSupportDepthBlocks) {
    public RoadTheme {
        Objects.requireNonNull(id, "id");
        fullBlocks = completeRoleMap(fullBlocks, "full block");
        bottomSlabs = completeRoleMap(bottomSlabs, "bottom slab");
        foundation = nonBlank(foundation, "foundation");
        support = nonBlank(support, "support");
        if (maximumSupportDepthBlocks < 1 || maximumSupportDepthBlocks > 256) {
            throw new IllegalArgumentException(
                    "Maximum road support depth must be between 1 and 256 blocks");
        }
    }

    private static Map<RoadSurfaceRole, String> completeRoleMap(
            Map<RoadSurfaceRole, String> source,
            String description) {
        Objects.requireNonNull(source, description + " map");
        EnumMap<RoadSurfaceRole, String> result = new EnumMap<>(RoadSurfaceRole.class);
        for (RoadSurfaceRole role : RoadSurfaceRole.values()) {
            String declaration = source.get(role);
            if (declaration == null) {
                throw new IllegalArgumentException(
                        "Missing " + description + " for road role " + role);
            }
            result.put(role, nonBlank(declaration, description + " for " + role));
        }
        if (source.size() != result.size()) {
            throw new IllegalArgumentException(description + " map contains unknown road roles");
        }
        return Map.copyOf(result);
    }

    private static String nonBlank(String value, String description) {
        Objects.requireNonNull(value, description);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Road theme " + description + " cannot be blank");
        }
        return value;
    }
}
