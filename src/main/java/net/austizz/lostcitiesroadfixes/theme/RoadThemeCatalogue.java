package net.austizz.lostcitiesroadfixes.theme;

import net.austizz.lostcitiesroadfixes.LostCitiesRoadFixes;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRole;

import java.util.EnumMap;

public final class RoadThemeCatalogue {
    public static final RoadThemeId DEFAULT_ID = new RoadThemeId(
            LostCitiesRoadFixes.MOD_ID, "default");
    private static final RoadTheme DEFAULT = createDefault();

    private RoadThemeCatalogue() {
    }

    public static RoadTheme defaultTheme() {
        return DEFAULT;
    }

    private static RoadTheme createDefault() {
        EnumMap<RoadSurfaceRole, String> full = new EnumMap<>(RoadSurfaceRole.class);
        EnumMap<RoadSurfaceRole, String> slabs = new EnumMap<>(RoadSurfaceRole.class);
        for (RoadSurfaceRole role : RoadSurfaceRole.values()) {
            full.put(role, switch (role) {
                case ASPHALT, AT_GRADE_INTERSECTION -> "minecraft:polished_deepslate";
                case SHOULDER, MEDIAN -> "minecraft:smooth_stone";
                case WHITE_MARKING -> "minecraft:quartz_block";
                case YELLOW_MARKING -> "minecraft:cut_sandstone";
            });
            slabs.put(role, switch (role) {
                case ASPHALT, AT_GRADE_INTERSECTION ->
                        "minecraft:polished_deepslate_slab[type=bottom,waterlogged=false]";
                case SHOULDER, MEDIAN ->
                        "minecraft:smooth_stone_slab[type=bottom,waterlogged=false]";
                case WHITE_MARKING ->
                        "minecraft:smooth_quartz_slab[type=bottom,waterlogged=false]";
                case YELLOW_MARKING ->
                        "minecraft:cut_sandstone_slab[type=bottom,waterlogged=false]";
            });
        }
        return new RoadTheme(
                DEFAULT_ID,
                full,
                slabs,
                "minecraft:smooth_stone",
                "minecraft:smooth_stone",
                40);
    }
}
