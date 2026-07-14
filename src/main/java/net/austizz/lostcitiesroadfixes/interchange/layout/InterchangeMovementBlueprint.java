package net.austizz.lostcitiesroadfixes.interchange.layout;

import java.util.Objects;

/**
 * A safe, site-independent declaration of one directed interchange movement.
 * The route form is compiled against surveyed native ports at generation time.
 */
public record InterchangeMovementBlueprint(
        InterchangeMovement movement,
        RampForm form,
        RampControl control,
        int widthBlocks,
        int structureLevel) {
    private static final int MAINLINE_CARRIAGEWAY_WIDTH_BLOCKS = 8;

    public InterchangeMovementBlueprint {
        Objects.requireNonNull(movement, "movement");
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(control, "control");
        if (widthBlocks < 3 || widthBlocks > 16) {
            throw new IllegalArgumentException(
                    "Movement width must be between 3 and 16 blocks");
        }
        if (structureLevel < 1) {
            throw new IllegalArgumentException("Movement structure level must be positive");
        }
        if (movement.kind() == MovementKind.STRAIGHT && form != RampForm.MAINLINE) {
            throw new IllegalArgumentException(
                    "Straight movement " + movement + " must use mainline form");
        }
        if (form == RampForm.MAINLINE
                && widthBlocks != MAINLINE_CARRIAGEWAY_WIDTH_BLOCKS) {
            throw new IllegalArgumentException(
                    "Mainline movement " + movement + " must be exactly "
                            + MAINLINE_CARRIAGEWAY_WIDTH_BLOCKS + " blocks wide");
        }
        if (movement.kind() != MovementKind.STRAIGHT && form == RampForm.MAINLINE) {
            throw new IllegalArgumentException(
                    "Turning movement " + movement + " cannot use mainline form");
        }
        if (form == RampForm.LOOP && movement.kind() != MovementKind.LEFT) {
            throw new IllegalArgumentException(
                    "Only left movements can use loop form: " + movement);
        }
    }
}
