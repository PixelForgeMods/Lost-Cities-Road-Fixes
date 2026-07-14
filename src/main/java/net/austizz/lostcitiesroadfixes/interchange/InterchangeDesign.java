package net.austizz.lostcitiesroadfixes.interchange;

import java.util.Objects;

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
        int constructionComplexity) {
    public InterchangeDesign {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(capacity, "capacity");
        if (minimumRadiusBlocks < 1 || minimumApproachRunBlocks < 1) {
            throw new IllegalArgumentException("Interchange dimensions must be positive");
        }
        if (requiredQuadrants < 1 || requiredQuadrants > 4) {
            throw new IllegalArgumentException("Required quadrants must be between one and four");
        }
        if (structureLevels < 1 || constructionComplexity < 1) {
            throw new IllegalArgumentException("Structure levels and complexity must be positive");
        }
        if (freeFlowMovementCount < 0) {
            throw new IllegalArgumentException("Free-flow movement count cannot be negative");
        }
    }
}
