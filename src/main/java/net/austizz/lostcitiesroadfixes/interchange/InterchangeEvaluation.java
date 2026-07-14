package net.austizz.lostcitiesroadfixes.interchange;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

public record InterchangeEvaluation(
        InterchangeDesign design,
        OptionalInt score,
        List<String> rejectionReasons) {
    public InterchangeEvaluation {
        Objects.requireNonNull(design, "design");
        Objects.requireNonNull(score, "score");
        rejectionReasons = List.copyOf(rejectionReasons);
        if (score.isPresent() == !rejectionReasons.isEmpty()) {
            throw new IllegalArgumentException(
                    "An interchange evaluation must have either a score or rejection reasons");
        }
    }

    public static InterchangeEvaluation feasible(InterchangeDesign design, int score) {
        return new InterchangeEvaluation(design, OptionalInt.of(score), List.of());
    }

    public static InterchangeEvaluation rejected(InterchangeDesign design, List<String> reasons) {
        if (reasons.isEmpty()) {
            throw new IllegalArgumentException("A rejected interchange needs at least one reason");
        }
        return new InterchangeEvaluation(design, OptionalInt.empty(), reasons);
    }

    public boolean feasible() {
        return score.isPresent();
    }
}
