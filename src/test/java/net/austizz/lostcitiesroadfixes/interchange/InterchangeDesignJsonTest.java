package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.interchange.layout.ApproachDirection;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeMovement;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeMovementBlueprint;
import net.austizz.lostcitiesroadfixes.interchange.layout.MovementKind;
import net.austizz.lostcitiesroadfixes.interchange.layout.RampControl;
import net.austizz.lostcitiesroadfixes.interchange.layout.RampForm;
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

    @Test
    void parsesACompleteFormatTwoMovementBlueprint() {
        InterchangeDesign design = InterchangeDesignJson.parse(ID, formatTwoJson());

        assertTrue(design.geometry().isPresent());
        assertEquals(12, design.geometry().orElseThrow().movements().size());
        InterchangeMovementBlueprint loop = design.geometry().orElseThrow().movements().stream()
                .filter(movement -> movement.movement().equals(new InterchangeMovement(
                        ApproachDirection.WEST,
                        ApproachDirection.NORTH,
                        MovementKind.LEFT)))
                .findFirst()
                .orElseThrow();
        assertEquals(RampForm.LOOP, loop.form());
        assertEquals(RampControl.FREE_FLOW, loop.control());
        assertEquals(10, loop.widthBlocks());
        assertEquals(2, loop.structureLevel());
    }

    @Test
    void rejectsIncompleteContradictoryAndUnknownFormatTwoGeometry() {
        IllegalArgumentException incomplete = assertThrows(IllegalArgumentException.class,
                () -> InterchangeDesignJson.parse(ID, formatTwoJson().replace(
                        ",\n    {\"from\":\"west\",\"to\":\"north\",\"form\":\"loop\",\"control\":\"free_flow\",\"width_blocks\":10,\"structure_level\":2}",
                        "")));
        assertTrue(incomplete.getMessage().contains(ID.toString()));
        assertTrue(incomplete.getMessage().contains("missing"));

        IllegalArgumentException contradictory = assertThrows(IllegalArgumentException.class,
                () -> InterchangeDesignJson.parse(ID, formatTwoJson().replace(
                        "\"free_flow_movement_count\": 5",
                        "\"free_flow_movement_count\": 4")));
        assertTrue(contradictory.getMessage().contains("free-flow"));

        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> InterchangeDesignJson.parse(ID, formatTwoJson().replaceFirst(
                        "\"width_blocks\":8",
                        "\"width_blocks\":8,\"spline\":true")));
        assertTrue(unknown.getMessage().contains("geometry.movements[0]"));
        assertTrue(unknown.getMessage().contains("spline"));
    }

    @Test
    void rejectsDuplicateUnavailableAndPhysicallyUnsafeMovementsWithTheResourceId() {
        IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class,
                () -> InterchangeDesignJson.parse(ID, formatTwoJson().replace(
                        "\"to\":\"north\",\"form\":\"loop\"",
                        "\"to\":\"south\",\"form\":\"direct\"")));
        assertTrue(duplicate.getMessage().contains(ID.toString()));
        assertTrue(duplicate.getMessage().contains("Duplicate"));

        IllegalArgumentException unavailable = assertThrows(IllegalArgumentException.class,
                () -> InterchangeDesignJson.parse(ID, formatTwoJson().replace(
                        "\"junction_form\": \"four_way\"",
                        "\"junction_form\": \"three_way\"")));
        assertTrue(unavailable.getMessage().contains(ID.toString()));
        assertTrue(unavailable.getMessage().contains("unavailable"));

        IllegalArgumentException width = assertThrows(IllegalArgumentException.class,
                () -> InterchangeDesignJson.parse(ID, formatTwoJson().replaceFirst(
                        "\"width_blocks\":8",
                        "\"width_blocks\":2")));
        assertTrue(width.getMessage().contains(ID.toString()));
        assertTrue(width.getMessage().contains("between 3 and 16"));

        IllegalArgumentException mainlineWidth = assertThrows(IllegalArgumentException.class,
                () -> InterchangeDesignJson.parse(ID, formatTwoJson().replaceFirst(
                        "\"width_blocks\":8",
                        "\"width_blocks\":9")));
        assertTrue(mainlineWidth.getMessage().contains(ID.toString()));
        assertTrue(mainlineWidth.getMessage().contains("exactly 8"));

        IllegalArgumentException routeForm = assertThrows(IllegalArgumentException.class,
                () -> InterchangeDesignJson.parse(ID, formatTwoJson().replaceFirst(
                        "\"form\":\"mainline\"",
                        "\"form\":\"direct\"")));
        assertTrue(routeForm.getMessage().contains(ID.toString()));
        assertTrue(routeForm.getMessage().contains("must use mainline"));

        IllegalArgumentException radius = assertThrows(IllegalArgumentException.class,
                () -> InterchangeDesignJson.parse(ID, formatTwoJson().replace(
                        "\"minimum_radius_blocks\": 64",
                        "\"minimum_radius_blocks\": 55")));
        assertTrue(radius.getMessage().contains(ID.toString()));
        assertTrue(radius.getMessage().contains("at least 24 blocks"));

        IllegalArgumentException approach = assertThrows(IllegalArgumentException.class,
                () -> InterchangeDesignJson.parse(ID, formatTwoJson().replace(
                        "\"minimum_approach_run_blocks\": 112",
                        "\"minimum_approach_run_blocks\": 64")));
        assertTrue(approach.getMessage().contains(ID.toString()));
        assertTrue(approach.getMessage().contains("must exceed"));
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

    private static String formatTwoJson() {
        return """
                {
                  "format": 2,
                  "family": "partial_cloverleaf",
                  "junction_form": "four_way",
                  "minimum_radius_blocks": 64,
                  "required_quadrants": 2,
                  "minimum_approach_run_blocks": 112,
                  "structure_levels": 2,
                  "uses_loop_ramps": true,
                  "all_movements_free_flow": false,
                  "capacity": "regional",
                  "free_flow_movement_count": 5,
                  "construction_complexity": 3,
                  "geometry": {
                    "movements": [
                    {"from":"north","to":"south","form":"mainline","control":"free_flow","width_blocks":8,"structure_level":1},
                    {"from":"north","to":"west","form":"direct","control":"yield","width_blocks":8,"structure_level":1},
                    {"from":"north","to":"east","form":"direct","control":"signalized","width_blocks":8,"structure_level":2},
                    {"from":"east","to":"west","form":"mainline","control":"free_flow","width_blocks":8,"structure_level":1},
                    {"from":"east","to":"north","form":"direct","control":"yield","width_blocks":8,"structure_level":1},
                    {"from":"east","to":"south","form":"direct","control":"signalized","width_blocks":8,"structure_level":2},
                    {"from":"south","to":"north","form":"mainline","control":"free_flow","width_blocks":8,"structure_level":1},
                    {"from":"south","to":"east","form":"direct","control":"yield","width_blocks":8,"structure_level":1},
                    {"from":"south","to":"west","form":"direct","control":"signalized","width_blocks":8,"structure_level":2},
                    {"from":"west","to":"east","form":"mainline","control":"free_flow","width_blocks":8,"structure_level":1},
                    {"from":"west","to":"south","form":"direct","control":"yield","width_blocks":8,"structure_level":1},
                    {"from":"west","to":"north","form":"loop","control":"free_flow","width_blocks":10,"structure_level":2}
                    ]
                  }
                }
                """;
    }
}
