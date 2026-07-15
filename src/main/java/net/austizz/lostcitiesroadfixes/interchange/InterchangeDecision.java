package net.austizz.lostcitiesroadfixes.interchange;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public record InterchangeDecision(
        InterchangeSite site,
        Optional<InterchangeDesign> selected,
        List<InterchangeEvaluation> evaluations) {
    public InterchangeDecision {
        Objects.requireNonNull(site, "site");
        Objects.requireNonNull(selected, "selected");
        evaluations = List.copyOf(evaluations);
    }

    public String diagnostic() {
        String choice = selected.map(design -> design.type().name()).orElse("none");
        String details = evaluations.stream()
                .map(evaluation -> evaluation.design().type() + "="
                        + (evaluation.score().isPresent()
                        ? "score " + evaluation.score().getAsInt()
                        : String.join(", ", evaluation.rejectionReasons())))
                .collect(Collectors.joining("; "));
        return "Selected " + choice + " for " + site + ": " + details;
    }

    public int selectedApproachRunBlocks() {
        InterchangeDesign design = selected.orElseThrow();
        return evaluations.stream()
                .filter(evaluation -> evaluation.design().equals(design))
                .findFirst()
                .orElseThrow()
                .approachRunBlocks()
                .orElseThrow();
    }
}
