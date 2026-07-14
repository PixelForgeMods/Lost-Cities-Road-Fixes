package net.austizz.lostcitiesroadfixes.interchange.layout;

import java.util.Objects;

public record InterchangeMovement(
        ApproachDirection from,
        ApproachDirection to,
        MovementKind kind) {
    public InterchangeMovement {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(kind, "kind");
        ApproachDirection expected = switch (kind) {
            case STRAIGHT -> from.opposite();
            case RIGHT -> from.rightTurnDestination();
            case LEFT -> from.leftTurnDestination();
        };
        if (to != expected) {
            throw new IllegalArgumentException(
                    kind + " from " + from + " must end at " + expected + ", not " + to);
        }
    }
}
