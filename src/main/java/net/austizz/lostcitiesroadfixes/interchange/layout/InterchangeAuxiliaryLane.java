package net.austizz.lostcitiesroadfixes.interchange.layout;

import net.austizz.lostcitiesroadfixes.interchange.geometry.RampRoute;

import java.util.Objects;

public record InterchangeAuxiliaryLane(
        InterchangeMovement mainlineMovement,
        RampRoute route) {
    public InterchangeAuxiliaryLane {
        Objects.requireNonNull(mainlineMovement, "mainlineMovement");
        Objects.requireNonNull(route, "route");
        if (mainlineMovement.kind() != MovementKind.STRAIGHT) {
            throw new IllegalArgumentException(
                    "An auxiliary lane must follow a straight mainline movement");
        }
    }
}
