package net.austizz.lostcitiesroadfixes.interchange;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

public record InterchangeEvaluation(
        InterchangeDesign design,
        OptionalInt score,
        OptionalInt approachRunBlocks,
        OptionalInt displacedBuildings,
        List<String> rejectionReasons) {
    public InterchangeEvaluation {
        Objects.requireNonNull(design, "design");
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(approachRunBlocks, "approachRunBlocks");
        Objects.requireNonNull(displacedBuildings, "displacedBuildings");
        rejectionReasons = List.copyOf(rejectionReasons);
        boolean feasible = score.isPresent();
        if (feasible == !rejectionReasons.isEmpty()
                || approachRunBlocks.isPresent() != feasible
                || displacedBuildings.isPresent() != feasible) {
            throw new IllegalArgumentException(
                    "An interchange evaluation must have either a score or rejection reasons");
        }
    }

    public static InterchangeEvaluation feasible(
            InterchangeDesign design,
            int score,
            int approachRunBlocks,
            int displacedBuildings) {
        return new InterchangeEvaluation(
                design,
                OptionalInt.of(score),
                OptionalInt.of(approachRunBlocks),
                OptionalInt.of(displacedBuildings),
                List.of());
    }

    public static InterchangeEvaluation rejected(InterchangeDesign design, List<String> reasons) {
        if (reasons.isEmpty()) {
            throw new IllegalArgumentException("A rejected interchange needs at least one reason");
        }
        return new InterchangeEvaluation(
                design,
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                reasons);
    }

    public boolean feasible() {
        return score.isPresent();
    }
}
