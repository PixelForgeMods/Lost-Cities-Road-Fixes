package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.interchange.layout.MovementKind;

import java.util.Objects;

/**
 * Shared planar measurements for the route primitives compiled by an
 * interchange design.
 */
public final class InterchangeRouteMetrics {
    private static final double LANE_OFFSET_BLOCKS = 8.0;
    private static final double QUARTER_TURN_RADIANS = StrictMath.PI / 2.0;

    private InterchangeRouteMetrics() {
    }

    public static double directTangentBlocks(
            InterchangeDesign design,
            int approachRunBlocks) {
        Objects.requireNonNull(design, "design");
        return approachRunBlocks - design.minimumRadiusBlocks();
    }

    public static double directRadiusBlocks(
            InterchangeDesign design,
            MovementKind movementKind) {
        Objects.requireNonNull(design, "design");
        Objects.requireNonNull(movementKind, "movementKind");
        return switch (movementKind) {
            case RIGHT -> design.minimumRadiusBlocks() - LANE_OFFSET_BLOCKS;
            case LEFT -> design.minimumRadiusBlocks() + LANE_OFFSET_BLOCKS;
            case STRAIGHT -> throw new IllegalArgumentException(
                    "A straight movement does not use direct-turn geometry");
        };
    }

    public static double directPreMergeRunBlocks(
            InterchangeDesign design,
            int approachRunBlocks,
            MovementKind movementKind) {
        double tangent = directTangentBlocks(design, approachRunBlocks);
        if (tangent < 0.0) {
            return 0.0;
        }
        return tangent
                + directRadiusBlocks(design, movementKind) * QUARTER_TURN_RADIANS;
    }

    public static double loopRadiusBlocks(InterchangeDesign design) {
        Objects.requireNonNull(design, "design");
        return (design.minimumRadiusBlocks() - LANE_OFFSET_BLOCKS) / 2.0;
    }

    public static double loopTangentBlocks(
            InterchangeDesign design,
            int approachRunBlocks) {
        return approachRunBlocks + LANE_OFFSET_BLOCKS + loopRadiusBlocks(design);
    }

    /**
     * Every complete three- or four-way layout contains a direct right turn.
     * It has the shortest usable run before joining its destination arterial,
     * so if it satisfies the grade then every longer direct or loop movement
     * can reach the destination elevation before its shared merge tangent.
     */
    public static int shortestTurningRouteRunBlocks(
            InterchangeDesign design,
            int approachRunBlocks) {
        double length = directPreMergeRunBlocks(
                design, approachRunBlocks, MovementKind.RIGHT);
        return length <= 0.0 ? 0 : (int) StrictMath.floor(length + 1.0e-9);
    }
}
