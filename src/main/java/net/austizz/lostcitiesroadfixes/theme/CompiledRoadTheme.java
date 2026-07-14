package net.austizz.lostcitiesroadfixes.theme;

import net.austizz.lostcitiesroadfixes.render.MinecraftRoadPalette;

import java.util.Objects;

public record CompiledRoadTheme(
        RoadThemeId id,
        MinecraftRoadPalette palette,
        int maximumSupportDepthBlocks) {
    public CompiledRoadTheme {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(palette, "palette");
        if (maximumSupportDepthBlocks < 1 || maximumSupportDepthBlocks > 256) {
            throw new IllegalArgumentException(
                    "Maximum road support depth must be between 1 and 256 blocks");
        }
    }
}
