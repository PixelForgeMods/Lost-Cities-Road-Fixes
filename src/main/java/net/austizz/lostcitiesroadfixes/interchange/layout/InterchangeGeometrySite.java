package net.austizz.lostcitiesroadfixes.interchange.layout;

import net.austizz.lostcitiesroadfixes.interchange.geometry.PlanarPoint;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RoadHeading;
import net.austizz.lostcitiesroadfixes.planning.elevation.GradeProfilePlanner;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;

import java.util.Objects;

public record InterchangeGeometrySite(
        PlanarPoint center,
        int approachRunBlocks,
        HalfBlockElevation xRoadNativeElevation,
        HalfBlockElevation zRoadNativeElevation,
        HalfBlockElevation xRoadCenterElevation,
        HalfBlockElevation zRoadCenterElevation) {
    private static final double LEGACY_LANE_OFFSET = 8.0;
    private static final double OUTER_THROUGH_LANE_OFFSET = 12.0;
    private static final double AUXILIARY_LANE_OFFSET = 21.0;
    private static final GradeProfilePlanner GRADE_PROFILE_PLANNER =
            new GradeProfilePlanner(RoadDesignStandard.DEFAULT);

    public InterchangeGeometrySite {
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(xRoadNativeElevation, "xRoadNativeElevation");
        Objects.requireNonNull(zRoadNativeElevation, "zRoadNativeElevation");
        Objects.requireNonNull(xRoadCenterElevation, "xRoadCenterElevation");
        Objects.requireNonNull(zRoadCenterElevation, "zRoadCenterElevation");
        if (approachRunBlocks < 1) {
            throw new IllegalArgumentException("Interchange approach run must be positive");
        }
    }

    public InterchangeGeometrySite(
            PlanarPoint center,
            int approachRunBlocks,
            HalfBlockElevation xRoadElevation,
            HalfBlockElevation zRoadElevation) {
        this(
                center,
                approachRunBlocks,
                xRoadElevation,
                zRoadElevation,
                xRoadElevation,
                zRoadElevation);
    }

    public InterchangePort port(ApproachDirection direction, TrafficFlow flow) {
        return portAt(direction, flow, approachRunBlocks, LEGACY_LANE_OFFSET);
    }

    public InterchangePort outerThroughPort(
            ApproachDirection direction,
            TrafficFlow flow) {
        return portAt(
                direction, flow, approachRunBlocks, OUTER_THROUGH_LANE_OFFSET);
    }

    public InterchangePort auxiliaryPort(
            ApproachDirection direction,
            TrafficFlow flow,
            int distanceFromCenter) {
        if (distanceFromCenter < 1 || distanceFromCenter > approachRunBlocks) {
            throw new IllegalArgumentException(
                    "Auxiliary terminal distance must be between 1 and "
                            + approachRunBlocks);
        }
        return portAt(direction, flow, distanceFromCenter, AUXILIARY_LANE_OFFSET);
    }

    private InterchangePort portAt(
            ApproachDirection direction,
            TrafficFlow flow,
            double run,
            double laneOffset) {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(flow, "flow");
        PlanarPoint localPoint;
        RoadHeading heading;
        switch (direction) {
            case NORTH -> {
                localPoint = flow == TrafficFlow.INBOUND
                        ? new PlanarPoint(-laneOffset, -run)
                        : new PlanarPoint(laneOffset, -run);
                heading = flow == TrafficFlow.INBOUND ? RoadHeading.SOUTH : RoadHeading.NORTH;
            }
            case EAST -> {
                localPoint = flow == TrafficFlow.INBOUND
                        ? new PlanarPoint(run, -laneOffset)
                        : new PlanarPoint(run, laneOffset);
                heading = flow == TrafficFlow.INBOUND ? RoadHeading.WEST : RoadHeading.EAST;
            }
            case SOUTH -> {
                localPoint = flow == TrafficFlow.INBOUND
                        ? new PlanarPoint(laneOffset, run)
                        : new PlanarPoint(-laneOffset, run);
                heading = flow == TrafficFlow.INBOUND ? RoadHeading.NORTH : RoadHeading.SOUTH;
            }
            case WEST -> {
                localPoint = flow == TrafficFlow.INBOUND
                        ? new PlanarPoint(-run, laneOffset)
                        : new PlanarPoint(-run, -laneOffset);
                heading = flow == TrafficFlow.INBOUND ? RoadHeading.EAST : RoadHeading.WEST;
            }
            default -> throw new IllegalStateException("Unhandled approach " + direction);
        }
        HalfBlockElevation elevation = approachElevation(direction, run);
        return new InterchangePort(
                direction,
                flow,
                new PlanarPoint(center.x() + localPoint.x(), center.z() + localPoint.z()),
                elevation,
                heading);
    }

    private HalfBlockElevation approachElevation(
            ApproachDirection direction,
            double distanceFromCenter) {
        HalfBlockElevation nativeElevation = switch (direction) {
            case EAST, WEST -> xRoadNativeElevation;
            case NORTH, SOUTH -> zRoadNativeElevation;
        };
        HalfBlockElevation centerElevation = centerElevation(direction);
        int inwardDistance = (int) StrictMath.floor(
                approachRunBlocks - distanceFromCenter + 1.0e-12);
        return GRADE_PROFILE_PLANNER.elevationOnMinimumRun(
                nativeElevation,
                centerElevation,
                inwardDistance);
    }

    public HalfBlockElevation centerElevation(ApproachDirection direction) {
        return switch (Objects.requireNonNull(direction, "direction")) {
            case EAST, WEST -> xRoadCenterElevation;
            case NORTH, SOUTH -> zRoadCenterElevation;
        };
    }
}
