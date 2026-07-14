package net.austizz.lostcitiesroadfixes.config;

import net.austizz.lostcitiesroadfixes.integration.RoadGenerationRuntime;
import net.austizz.lostcitiesroadfixes.theme.RoadThemeCatalogue;
import net.austizz.lostcitiesroadfixes.theme.RoadThemeId;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class RoadFixesServerConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue MAXIMUM_GAP_CHUNKS;
    private static final ModConfigSpec.IntValue MAXIMUM_CACHED_REGIONS;
    private static final ModConfigSpec.ConfigValue<String> ACTIVE_ROAD_THEME;
    private static final ModConfigSpec.BooleanValue LOG_FIRST_INTERCHANGE_SELECTION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(
                "Lost Cities: Road Fixes server settings.",
                "Safety-critical road width, grade, curve radius, clearance, and continuity",
                "cannot be disabled or weakened here.").push("roads");
        MAXIMUM_GAP_CHUNKS = builder
                .comment(
                        "Largest collinear missing-road run repaired by the continuity planner.",
                        "At least one chunk is always repaired; four is the verified upper bound.")
                .defineInRange(
                        "maximumGapChunks",
                        RoadOperationalSettings.MAXIMUM_GAP_CHUNKS,
                        RoadOperationalSettings.MINIMUM_GAP_CHUNKS,
                        RoadOperationalSettings.MAXIMUM_GAP_CHUNKS);
        MAXIMUM_CACHED_REGIONS = builder
                .comment(
                        "Maximum entries retained independently by each regional plan cache.",
                        "Evicted plans are recomputed deterministically when needed again.")
                .defineInRange(
                        "maximumCachedRegions",
                        512,
                        RoadOperationalSettings.MINIMUM_CACHED_REGIONS,
                        RoadOperationalSettings.MAXIMUM_CACHED_REGIONS);
        ACTIVE_ROAD_THEME = builder
                .comment(
                        "Namespaced loaded road theme used for newly generated chunks.",
                        "An unavailable ID falls back to lostcitiesroadfixes:default.")
                .define(
                        "activeRoadTheme",
                        RoadThemeCatalogue.DEFAULT_ID.toString(),
                        RoadFixesServerConfig::validThemeId);
        LOG_FIRST_INTERCHANGE_SELECTION = builder
                .comment("Log the first calculated interchange selected after server startup.")
                .define("logFirstInterchangeSelection", true);
        builder.pop();
        SPEC = builder.build();
    }

    private RoadFixesServerConfig() {
    }

    public static RoadOperationalSettings settings() {
        return new RoadOperationalSettings(
                MAXIMUM_GAP_CHUNKS.get(),
                MAXIMUM_CACHED_REGIONS.get(),
                RoadThemeId.parse(ACTIVE_ROAD_THEME.get()),
                LOG_FIRST_INTERCHANGE_SELECTION.get());
    }

    public static void onLoading(ModConfigEvent.Loading event) {
        invalidateIfOurs(event);
    }

    public static void onReloading(ModConfigEvent.Reloading event) {
        invalidateIfOurs(event);
    }

    private static void invalidateIfOurs(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            RoadGenerationRuntime.invalidatePlans();
        }
    }

    private static boolean validThemeId(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        try {
            RoadThemeId.parse(text);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
