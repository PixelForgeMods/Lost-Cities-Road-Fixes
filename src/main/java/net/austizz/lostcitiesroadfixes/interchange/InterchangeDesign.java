package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeGeometryBlueprint;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeMovementBlueprint;
import net.austizz.lostcitiesroadfixes.interchange.layout.MovementKind;
import net.austizz.lostcitiesroadfixes.interchange.layout.RampForm;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import net.austizz.lostcitiesroadfixes.road.RoadKind;

import java.util.Objects;
import java.util.Optional;

public record InterchangeDesign(
        InterchangeDesignId id,
        InterchangeType type,
        JunctionForm form,
        int minimumRadiusBlocks,
        int requiredQuadrants,
        int minimumApproachRunBlocks,
        int structureLevels,
        boolean usesLoopRamps,
        boolean allMovementsFreeFlow,
        TrafficDemand capacity,
        int freeFlowMovementCount,
        int constructionComplexity,
        Optional<InterchangeGeometryBlueprint> geometry) {
    private static final double LANE_OFFSET_BLOCKS = 8.0;

    public InterchangeDesign(
            InterchangeDesignId id,
            InterchangeType type,
            JunctionForm form,
            int minimumRadiusBlocks,
            int requiredQuadrants,
            int minimumApproachRunBlocks,
            int structureLevels,
            boolean usesLoopRamps,
            boolean allMovementsFreeFlow,
            TrafficDemand capacity,
            int freeFlowMovementCount,
            int constructionComplexity) {
        this(
                id,
                type,
                form,
                minimumRadiusBlocks,
                requiredQuadrants,
                minimumApproachRunBlocks,
                structureLevels,
                usesLoopRamps,
                allMovementsFreeFlow,
                capacity,
                freeFlowMovementCount,
                constructionComplexity,
                Optional.empty());
    }

    public InterchangeDesign {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(capacity, "capacity");
        geometry = Objects.requireNonNull(geometry, "geometry");
        if (minimumRadiusBlocks < 1 || minimumApproachRunBlocks < 1) {
            throw new IllegalArgumentException("Interchange dimensions must be positive");
        }
        if (requiredQuadrants < 1 || requiredQuadrants > 4) {
            throw new IllegalArgumentException("Required quadrants must be between one and four");
        }
        if (structureLevels < 1 || constructionComplexity < 1) {
            throw new IllegalArgumentException("Structure levels and complexity must be positive");
        }
        int movementCount = form == JunctionForm.THREE_WAY ? 6 : 12;
        if (freeFlowMovementCount < 0 || freeFlowMovementCount > movementCount) {
            throw new IllegalArgumentException(
                    "Free-flow movement count must be between zero and " + movementCount);
        }
        if (allMovementsFreeFlow && freeFlowMovementCount != movementCount) {
            throw new IllegalArgumentException(
                    "An all-free-flow design must declare all " + movementCount + " movements");
        }
        if (geometry.isPresent() && minimumApproachRunBlocks <= minimumRadiusBlocks) {
            throw new IllegalArgumentException(
                    "Custom geometry approach run must exceed its nominal radius");
        }
        geometry.ifPresent(blueprint -> {
            blueprint.validateFor(
                    form,
                    structureLevels,
                    usesLoopRamps,
                    allMovementsFreeFlow,
                    freeFlowMovementCount);
            validateCurveRadii(blueprint, minimumRadiusBlocks);
        });
    }

    private static void validateCurveRadii(
            InterchangeGeometryBlueprint geometry,
            int nominalRadiusBlocks) {
        RoadDesignStandard standard = RoadDesignStandard.DEFAULT;
        for (InterchangeMovementBlueprint blueprint : geometry.movements()) {
            if (blueprint.form() == RampForm.MAINLINE) {
                continue;
            }
            double centerlineRadius = blueprint.form() == RampForm.LOOP
                    ? (nominalRadiusBlocks - LANE_OFFSET_BLOCKS) / 2.0
                    : blueprint.movement().kind() == MovementKind.RIGHT
                            ? nominalRadiusBlocks - LANE_OFFSET_BLOCKS
                            : nominalRadiusBlocks + LANE_OFFSET_BLOCKS;
            if (centerlineRadius <= 0.0) {
                throw new IllegalArgumentException(
                        "Movement " + blueprint.movement()
                                + " has a non-positive calculated curve radius");
            }
            standard.requireCurveRadius(
                    RoadKind.RAMP,
                    (int) StrictMath.floor(centerlineRadius));
        }
    }
}
