package net.austizz.lostcitiesroadfixes.theme;

import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadThemeJsonTest {
    private static final RoadThemeId ID = RoadThemeId.parse("example:copper_roads");

    @Test
    void parsesEverySemanticMaterialAndPreservesBlockStateProperties() {
        RoadTheme theme = RoadThemeJson.parse(ID, validJson());

        assertEquals(ID, theme.id());
        assertEquals("minecraft:black_concrete",
                theme.fullBlocks().get(RoadSurfaceRole.ASPHALT));
        assertEquals("minecraft:oxidized_cut_copper_slab[type=bottom,waterlogged=false]",
                theme.bottomSlabs().get(RoadSurfaceRole.ASPHALT));
        assertEquals("minecraft:iron_block", theme.foundation());
        assertEquals("minecraft:deepslate_bricks", theme.support());
        assertEquals(40, theme.maximumSupportDepthBlocks());
    }

    @Test
    void rejectsIncompleteUnknownAndInvalidScalarDeclarationsWithTheResourceId() {
        IllegalArgumentException missingRole = assertThrows(IllegalArgumentException.class,
                () -> RoadThemeJson.parse(ID, validJson().replace(
                        "    \"median\": \"minecraft:smooth_stone\",\n",
                        "")));
        assertTrue(missingRole.getMessage().contains(ID.toString()));
        assertTrue(missingRole.getMessage().contains("median"));

        IllegalArgumentException unknownField = assertThrows(IllegalArgumentException.class,
                () -> RoadThemeJson.parse(ID, validJson().replace(
                        "\"maximum_support_depth_blocks\": 40",
                        "\"maximum_support_depth_blocks\": 40, \"pillars\": true")));
        assertTrue(unknownField.getMessage().contains("pillars"));

        IllegalArgumentException depth = assertThrows(IllegalArgumentException.class,
                () -> RoadThemeJson.parse(ID, validJson().replace(
                        "\"maximum_support_depth_blocks\": 40",
                        "\"maximum_support_depth_blocks\": 0")));
        assertTrue(depth.getMessage().contains(ID.toString()));
        assertTrue(depth.getMessage().contains("between 1 and 256"));
    }

    static String validJson() {
        return """
                {
                  "format": 1,
                  "full_blocks": {
                    "shoulder": "minecraft:stone_bricks",
                    "asphalt": "minecraft:black_concrete",
                    "white_marking": "minecraft:quartz_block",
                    "yellow_marking": "minecraft:yellow_concrete",
                    "median": "minecraft:smooth_stone",
                    "at_grade_intersection": "minecraft:gray_concrete"
                  },
                  "bottom_slabs": {
                    "shoulder": "minecraft:stone_brick_slab[type=bottom,waterlogged=false]",
                    "asphalt": "minecraft:oxidized_cut_copper_slab[type=bottom,waterlogged=false]",
                    "white_marking": "minecraft:smooth_quartz_slab[type=bottom,waterlogged=false]",
                    "yellow_marking": "minecraft:cut_sandstone_slab[type=bottom,waterlogged=false]",
                    "median": "minecraft:smooth_stone_slab[type=bottom,waterlogged=false]",
                    "at_grade_intersection": "minecraft:polished_blackstone_brick_slab[type=bottom,waterlogged=false]"
                  },
                  "foundation": "minecraft:iron_block",
                  "support": "minecraft:deepslate_bricks",
                  "maximum_support_depth_blocks": 40
                }
                """;
    }
}
