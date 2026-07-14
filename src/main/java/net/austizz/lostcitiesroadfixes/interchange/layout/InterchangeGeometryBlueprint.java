package net.austizz.lostcitiesroadfixes.interchange.layout;

import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * The complete canonical movement contract for a custom interchange design.
 */
public record InterchangeGeometryBlueprint(List<InterchangeMovementBlueprint> movements) {
    private static final Comparator<InterchangeMovementBlueprint> ORDER = Comparator
            .comparingInt((InterchangeMovementBlueprint blueprint) ->
                    blueprint.movement().from().ordinal())
            .thenComparingInt(blueprint -> blueprint.movement().kind().ordinal());

    public InterchangeGeometryBlueprint {
        Objects.requireNonNull(movements, "movements");
        List<InterchangeMovementBlueprint> ordered = new ArrayList<>(movements.size());
        Set<InterchangeMovement> unique = new HashSet<>();
        for (InterchangeMovementBlueprint movement : movements) {
            Objects.requireNonNull(movement, "movement blueprint");
            if (!unique.add(movement.movement())) {
                throw new IllegalArgumentException(
                        "Duplicate interchange movement " + movement.movement());
            }
            ordered.add(movement);
        }
        ordered.sort(ORDER);
        movements = List.copyOf(ordered);
    }

    public void validateFor(
            JunctionForm form,
            int structureLevels,
            boolean usesLoopRamps,
            boolean allMovementsFreeFlow,
            int freeFlowMovementCount) {
        Objects.requireNonNull(form, "form");
        Set<InterchangeMovement> expected = expectedMovements(form);
        Set<InterchangeMovement> actual = new HashSet<>();
        for (InterchangeMovementBlueprint blueprint : movements) {
            actual.add(blueprint.movement());
            if (blueprint.structureLevel() > structureLevels) {
                throw new IllegalArgumentException(
                        "Movement " + blueprint.movement() + " uses structure level "
                                + blueprint.structureLevel() + " but the design declares "
                                + structureLevels);
            }
        }

        Set<InterchangeMovement> missing = new HashSet<>(expected);
        missing.removeAll(actual);
        Set<InterchangeMovement> unavailable = new HashSet<>(actual);
        unavailable.removeAll(expected);
        if (!missing.isEmpty() || !unavailable.isEmpty()) {
            throw new IllegalArgumentException(
                    "Movement blueprint does not match " + form.name().toLowerCase()
                            + ": missing " + orderedDescriptions(missing)
                            + ", unavailable " + orderedDescriptions(unavailable));
        }

        boolean actualLoops = movements.stream()
                .anyMatch(blueprint -> blueprint.form() == RampForm.LOOP);
        if (usesLoopRamps != actualLoops) {
            throw new IllegalArgumentException(
                    "Loop-ramp metadata does not match the movement blueprint");
        }
        long actualFreeFlow = movements.stream()
                .filter(blueprint -> blueprint.control() == RampControl.FREE_FLOW)
                .count();
        if (freeFlowMovementCount != actualFreeFlow) {
            throw new IllegalArgumentException(
                    "Declared free-flow movement count " + freeFlowMovementCount
                            + " does not match blueprint count " + actualFreeFlow);
        }
        boolean actualAllFreeFlow = actualFreeFlow == movements.size();
        if (allMovementsFreeFlow != actualAllFreeFlow) {
            throw new IllegalArgumentException(
                    "All-movements-free-flow metadata does not match the movement blueprint");
        }
        int actualLevels = movements.stream()
                .mapToInt(InterchangeMovementBlueprint::structureLevel)
                .max()
                .orElse(0);
        if (actualLevels != structureLevels) {
            throw new IllegalArgumentException(
                    "Declared structure levels " + structureLevels
                            + " do not match blueprint maximum " + actualLevels);
        }
    }

    private static Set<InterchangeMovement> expectedMovements(JunctionForm form) {
        Set<ApproachDirection> approaches = form == JunctionForm.THREE_WAY
                ? EnumSet.of(
                        ApproachDirection.WEST,
                        ApproachDirection.EAST,
                        ApproachDirection.SOUTH)
                : EnumSet.allOf(ApproachDirection.class);
        Set<InterchangeMovement> result = new HashSet<>();
        for (ApproachDirection from : approaches) {
            for (MovementKind kind : MovementKind.values()) {
                ApproachDirection to = destination(from, kind);
                if (approaches.contains(to)) {
                    result.add(new InterchangeMovement(from, to, kind));
                }
            }
        }
        return Set.copyOf(result);
    }

    private static ApproachDirection destination(
            ApproachDirection from,
            MovementKind kind) {
        return switch (kind) {
            case STRAIGHT -> from.opposite();
            case RIGHT -> from.rightTurnDestination();
            case LEFT -> from.leftTurnDestination();
        };
    }

    private static Set<String> orderedDescriptions(Set<InterchangeMovement> movements) {
        Set<String> descriptions = new TreeSet<>();
        movements.forEach(movement -> descriptions.add(
                movement.from().name().toLowerCase() + "->"
                        + movement.to().name().toLowerCase()));
        return descriptions;
    }
}
