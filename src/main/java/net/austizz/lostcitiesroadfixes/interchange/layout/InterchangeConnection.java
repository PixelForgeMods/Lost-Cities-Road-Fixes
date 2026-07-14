package net.austizz.lostcitiesroadfixes.interchange.layout;

import net.austizz.lostcitiesroadfixes.interchange.geometry.RampRoute;

import java.util.Objects;

public record InterchangeConnection(
        InterchangeMovement movement,
        RampRoute route,
        RampForm form,
        RampControl control,
        int structureLevel) {
    public InterchangeConnection {
        Objects.requireNonNull(movement, "movement");
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(control, "control");
        if (structureLevel < 1) {
            throw new IllegalArgumentException("Structure level must be positive");
        }
        if (movement.kind() == MovementKind.STRAIGHT && form != RampForm.MAINLINE) {
            throw new IllegalArgumentException("Straight movement must use a mainline route");
        }
        if (movement.kind() != MovementKind.STRAIGHT && form == RampForm.MAINLINE) {
            throw new IllegalArgumentException("Turning movement cannot use a mainline route");
        }
    }
}
