package net.austizz.lostcitiesroadfixes.interchange.render;

import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
import net.austizz.lostcitiesroadfixes.render.ElevatedRoadTile;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;

import java.util.Objects;

public record GradedArterial(
        RoadAxis axis,
        int centerBlockX,
        int centerBlockZ,
        int approachRunBlocks,
        boolean negativeArm,
        boolean positiveArm,
        HalfBlockElevation nativeElevation,
        HalfBlockElevation centerElevation) {
    public GradedArterial {
        Objects.requireNonNull(axis, "axis");
        Objects.requireNonNull(nativeElevation, "nativeElevation");
        Objects.requireNonNull(centerElevation, "centerElevation");
        if (approachRunBlocks < 1) {
            throw new IllegalArgumentException("Graded arterial approach must be positive");
        }
        if (!negativeArm && !positiveArm) {
            throw new IllegalArgumentException("Graded arterial requires at least one arm");
        }
    }

    public int centerLongitudinalBlock() {
        return axis == RoadAxis.X ? centerBlockX : centerBlockZ;
    }

    public int centerCrossBlock() {
        return axis == RoadAxis.X ? centerBlockZ : centerBlockX;
    }

    public boolean containsLongitudinal(int blockCoordinate) {
        int offset = blockCoordinate - centerLongitudinalBlock();
        if (StrictMath.abs((long) offset) > approachRunBlocks) {
            return false;
        }
        if (offset == 0) {
            return true;
        }
        return offset < 0 ? negativeArm : positiveArm;
    }

    public HalfBlockElevation elevationAt(int blockCoordinate) {
        if (!containsLongitudinal(blockCoordinate)) {
            throw new IllegalArgumentException("Block is outside graded arterial arms");
        }
        int distance = StrictMath.abs(blockCoordinate - centerLongitudinalBlock());
        if (distance == 0) {
            return centerElevation;
        }
        long delta = (long) centerElevation.halfBlocks() - nativeElevation.halfBlocks();
        long progress = (long) StrictMath.floor(
                (approachRunBlocks - distance) * Math.abs(delta)
                        / (double) approachRunBlocks + 1.0e-12);
        long halfBlocks = delta >= 0
                ? (long) nativeElevation.halfBlocks() + progress
                : (long) nativeElevation.halfBlocks() - progress;
        return new HalfBlockElevation(Math.toIntExact(halfBlocks));
    }

    public boolean replaces(ElevatedRoadTile road) {
        Objects.requireNonNull(road, "road");
        if (road.axis() != axis || !road.elevation().equals(nativeElevation)) {
            return false;
        }
        int roadCrossCenter = axis == RoadAxis.X
                ? Math.addExact(Math.multiplyExact(road.chunk().z(), 16), 8)
                : Math.addExact(Math.multiplyExact(road.chunk().x(), 16), 8);
        if (roadCrossCenter != centerCrossBlock()) {
            return false;
        }
        int minimum = axis == RoadAxis.X ? road.chunk().minBlockX() : road.chunk().minBlockZ();
        int maximum = axis == RoadAxis.X ? road.chunk().maxBlockX() : road.chunk().maxBlockZ();
        for (int coordinate = minimum; coordinate <= maximum; coordinate++) {
            if (containsLongitudinal(coordinate)) {
                return true;
            }
        }
        return false;
    }
}
