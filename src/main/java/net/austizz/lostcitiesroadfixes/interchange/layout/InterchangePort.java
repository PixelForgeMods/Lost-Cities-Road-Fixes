package net.austizz.lostcitiesroadfixes.interchange.layout;

import net.austizz.lostcitiesroadfixes.interchange.geometry.PlanarPoint;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RoadHeading;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;

import java.util.Objects;

public record InterchangePort(
        ApproachDirection direction,
        TrafficFlow flow,
        PlanarPoint point,
        HalfBlockElevation elevation,
        RoadHeading heading) {
    public InterchangePort {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(flow, "flow");
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(elevation, "elevation");
        Objects.requireNonNull(heading, "heading");
    }
}
