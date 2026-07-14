package net.austizz.lostcitiesroadfixes.theme;

import net.austizz.lostcitiesroadfixes.render.MinecraftRoadPalette;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRole;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Objects;

public final class RoadThemeCompiler {
    public CompiledRoadTheme compile(RoadTheme theme) {
        Objects.requireNonNull(theme, "theme");
        try {
            EnumMap<RoadSurfaceRole, BlockState> full = new EnumMap<>(RoadSurfaceRole.class);
            EnumMap<RoadSurfaceRole, BlockState> slabs = new EnumMap<>(RoadSurfaceRole.class);
            for (RoadSurfaceRole role : RoadSurfaceRole.values()) {
                String key = role.name().toLowerCase(java.util.Locale.ROOT);
                full.put(role, parse(
                        theme, "full_blocks." + key, theme.fullBlocks().get(role)));
                slabs.put(role, parse(
                        theme, "bottom_slabs." + key, theme.bottomSlabs().get(role)));
            }
            MinecraftRoadPalette palette = new MinecraftRoadPalette(
                    full,
                    slabs,
                    parse(theme, "foundation", theme.foundation()),
                    parse(theme, "support", theme.support()));
            return new CompiledRoadTheme(
                    theme.id(), palette, theme.maximumSupportDepthBlocks());
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null
                    && exception.getMessage().contains(theme.id().toString())) {
                throw exception;
            }
            throw new IllegalArgumentException(
                    "Invalid road theme " + theme.id() + ": " + exception.getMessage(),
                    exception);
        }
    }

    private static BlockState parse(
            RoadTheme theme,
            String path,
            String declaration) {
        try {
            return BlockStateTextParser.parse(declaration);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid road theme " + theme.id() + " field '" + path
                            + "': " + exception.getMessage(),
                    exception);
        }
    }
}
