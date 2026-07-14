package net.austizz.lostcitiesroadfixes.interchange;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterchangeDesignJsonTest {
    private static final InterchangeDesignId ID = InterchangeDesignId.parse("example:compact_diamond");

    @Test
    void parsesEverySelectorPropertyUsingTheResourceId() {
        InterchangeDesign design = InterchangeDesignJson.parse(ID, """
                {
                  "format": 1,
                  "family": "diamond",
                  "junction_form": "four_way",
                  "minimum_radius_blocks": 60,
                  "required_quadrants": 2,
                  "minimum_approach_run_blocks": 112,
                  "structure_levels": 2,
                  "uses_loop_ramps": false,
                  "all_movements_free_flow": false,
                  "capacity": "regional",
                  "free_flow_movement_count": 2,
                  "construction_complexity": 3
                }
                """);

        assertEquals(ID, design.id());
        assertEquals(InterchangeType.DIAMOND, design.type());
        assertEquals(JunctionForm.FOUR_WAY, design.form());
        assertEquals(60, design.minimumRadiusBlocks());
        assertEquals(2, design.requiredQuadrants());
        assertEquals(112, design.minimumApproachRunBlocks());
        assertEquals(2, design.structureLevels());
        assertEquals(false, design.usesLoopRamps());
        assertEquals(false, design.allMovementsFreeFlow());
        assertEquals(TrafficDemand.REGIONAL, design.capacity());
        assertEquals(2, design.freeFlowMovementCount());
        assertEquals(3, design.constructionComplexity());
    }

    @Test
    void reportsTheResourceAndMissingField() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> InterchangeDesignJson.parse(ID, "{}"));

        assertTrue(failure.getMessage().contains("example:compact_diamond"));
        assertTrue(failure.getMessage().contains("format"));
    }

    @Test
    void rejectsUnknownFieldsAndInvalidDomainValues() {
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> InterchangeDesignJson.parse(ID, validJson()
                        .replace("\n}", ",\n  \"raduis\": 64\n}")));
        assertTrue(unknown.getMessage().contains("raduis"));

        IllegalArgumentException invalid = assertThrows(IllegalArgumentException.class,
                () -> InterchangeDesignJson.parse(ID, validJson()
                        .replace("\"required_quadrants\": 2", "\"required_quadrants\": 5")));
        assertTrue(invalid.getMessage().contains("example:compact_diamond"));
        assertTrue(invalid.getMessage().contains("quadrants"));
    }

    private static String validJson() {
        return """
                {
                  "format": 1,
                  "family": "diamond",
                  "junction_form": "four_way",
                  "minimum_radius_blocks": 60,
                  "required_quadrants": 2,
                  "minimum_approach_run_blocks": 112,
                  "structure_levels": 2,
                  "uses_loop_ramps": false,
                  "all_movements_free_flow": false,
                  "capacity": "regional",
                  "free_flow_movement_count": 2,
                  "construction_complexity": 3
                }
                """;
    }
}
