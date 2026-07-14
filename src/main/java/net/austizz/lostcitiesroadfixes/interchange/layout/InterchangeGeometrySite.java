package net.austizz.lostcitiesroadfixes.interchange.layout;

import net.austizz.lostcitiesroadfixes.interchange.geometry.PlanarPoint;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RoadHeading;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;

import java.util.Objects;

public record InterchangeGeometrySite(
        PlanarPoint center,
        int approachRunBlocks,
        HalfBlockElevation xRoadElevation,
        HalfBlockElevation zRoadElevation) {
    private static final double LANE_OFFSET = 8.0;

    public InterchangeGeometrySite {
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(xRoadElevation, "xRoadElevation");
        Objects.requireNonNull(zRoadElevation, "zRoadElevation");
        if (approachRunBlocks < 1) {
            throw new IllegalArgumentException("Interchange approach run must be positive");
        }
    }

    public InterchangePort port(ApproachDirection direction, TrafficFlow flow) {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(flow, "flow");
        double run = approachRunBlocks;
        PlanarPoint localPoint;
        RoadHeading heading;
        switch (direction) {
            case NORTH -> {
                localPoint = flow == TrafficFlow.INBOUND
                        ? new PlanarPoint(-LANE_OFFSET, -run)
                        : new PlanarPoint(LANE_OFFSET, -run);
                heading = flow == TrafficFlow.INBOUND ? RoadHeading.SOUTH : RoadHeading.NORTH;
            }
            case EAST -> {
                localPoint = flow == TrafficFlow.INBOUND
                        ? new PlanarPoint(run, -LANE_OFFSET)
                        : new PlanarPoint(run, LANE_OFFSET);
                heading = flow == TrafficFlow.INBOUND ? RoadHeading.WEST : RoadHeading.EAST;
            }
            case SOUTH -> {
                localPoint = flow == TrafficFlow.INBOUND
                        ? new PlanarPoint(LANE_OFFSET, run)
                        : new PlanarPoint(-LANE_OFFSET, run);
                heading = flow == TrafficFlow.INBOUND ? RoadHeading.NORTH : RoadHeading.SOUTH;
            }
            case WEST -> {
                localPoint = flow == TrafficFlow.INBOUND
                        ? new PlanarPoint(-run, LANE_OFFSET)
                        : new PlanarPoint(-run, -LANE_OFFSET);
                heading = flow == TrafficFlow.INBOUND ? RoadHeading.EAST : RoadHeading.WEST;
            }
            default -> throw new IllegalStateException("Unhandled approach " + direction);
        }
        HalfBlockElevation elevation = switch (direction) {
            case EAST, WEST -> xRoadElevation;
            case NORTH, SOUTH -> zRoadElevation;
        };
        return new InterchangePort(
                direction,
                flow,
                new PlanarPoint(center.x() + localPoint.x(), center.z() + localPoint.z()),
                elevation,
                heading);
    }
}
