package net.austizz.lostcitiesroadfixes.diagnostics;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesign;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeEvaluation;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeSite;
import net.austizz.lostcitiesroadfixes.interchange.planning.ConflictedRoadCrossing;
import net.austizz.lostcitiesroadfixes.interchange.planning.CrossingDecks;
import net.austizz.lostcitiesroadfixes.interchange.planning.DetectedRoadCrossing;
import net.austizz.lostcitiesroadfixes.interchange.planning.PlannedInterchange;
import net.austizz.lostcitiesroadfixes.interchange.planning.RejectedRoadCrossing;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** A stable, operator-facing explanation of one surveyed crossing decision. */
public record InterchangeExplanation(
        ChunkPoint chunk,
        Outcome outcome,
        List<String> details) {
    public InterchangeExplanation {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(outcome, "outcome");
        details = List.copyOf(Objects.requireNonNull(details, "details"));
        if (details.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Explanation details cannot be blank");
        }
    }

    public static InterchangeExplanation selected(PlannedInterchange interchange) {
        Objects.requireNonNull(interchange, "interchange");
        DetectedRoadCrossing crossing = interchange.crossing();
        InterchangeDesign design = interchange.decision().selected().orElseThrow();
        InterchangeEvaluation evaluation = interchange.decision().evaluations().stream()
                .filter(candidate -> candidate.design().equals(design))
                .findFirst()
                .orElseThrow();
        List<String> details = new ArrayList<>(siteDetails(crossing));
        details.add(0, "design: id=" + design.id() + ", family="
                + design.type().name().toLowerCase(Locale.ROOT));
        details.add("selection: compiledApproachBlocks="
                + evaluation.approachRunBlocks().orElseThrow()
                + ", displacedBuildings="
                + evaluation.displacedBuildings().orElseThrow()
                + ", score=" + evaluation.score().orElseThrow());
        return new InterchangeExplanation(crossing.chunk(), Outcome.SELECTED, details);
    }

    public static InterchangeExplanation rejected(RejectedRoadCrossing rejected) {
        Objects.requireNonNull(rejected, "rejected");
        List<String> details = new ArrayList<>(siteDetails(rejected.crossing()));
        for (InterchangeEvaluation evaluation : rejected.decision().evaluations()) {
            details.add("candidate " + evaluation.design().id() + ": "
                    + String.join("; ", evaluation.rejectionReasons()));
        }
        return new InterchangeExplanation(
                rejected.crossing().chunk(), Outcome.REJECTED, details);
    }

    public static InterchangeExplanation conflicted(ConflictedRoadCrossing conflict) {
        Objects.requireNonNull(conflict, "conflict");
        InterchangeDesign design = conflict.decision().selected().orElseThrow();
        List<String> details = new ArrayList<>(siteDetails(conflict.crossing()));
        details.add(0, "candidate: id=" + design.id() + ", family="
                + design.type().name().toLowerCase(Locale.ROOT));
        details.add("conflict: blocker=" + conflict.blockingCrossing().x() + ','
                + conflict.blockingCrossing().z()
                + ", requiredCenterSeparationBlocks="
                + conflict.minimumCenterSeparationBlocks());
        return new InterchangeExplanation(
                conflict.crossing().chunk(), Outcome.CONFLICTED, details);
    }

    public static InterchangeExplanation none(ChunkPoint chunk) {
        return new InterchangeExplanation(
                chunk,
                Outcome.NO_CROSSING,
                List.of("No differing-height Lost Cities road crossing was detected in this chunk"));
    }

    public List<String> lines() {
        List<String> result = new ArrayList<>(details.size() + 2);
        result.add("Interchange explanation for chunk " + chunk.x() + ',' + chunk.z());
        result.add("outcome: " + outcome.name().toLowerCase(Locale.ROOT));
        result.addAll(details);
        return List.copyOf(result);
    }

    private static List<String> siteDetails(DetectedRoadCrossing crossing) {
        InterchangeSite site = crossing.selectionSite();
        CrossingDecks decks = crossing.decks();
        int gapHalfBlocks = decks.upperPlannedDeck().halfBlocks()
                - decks.lowerPlannedDeck().halfBlocks();
        return List.of(
                "site: form=" + crossing.form().name().toLowerCase(Locale.ROOT)
                        + ", demand=" + crossing.demand().name().toLowerCase(Locale.ROOT)
                        + ", approaches=" + crossing.approaches().size()
                        + ", surveyedApproachBlocks=" + crossing.approachRunBlocks(),
                "space: radiusBlocks=" + crossing.availableRadiusBlocks()
                        + ", availableQuadrants=" + crossing.availableQuadrants()
                        + ", structureLevels=" + crossing.maximumStructureLevels()
                        + ", buildings=" + crossing.environment().buildingFootprints().size(),
                "elevations: nativeX=" + format(decks.nativeX())
                        + ", nativeZ=" + format(decks.nativeZ())
                        + ", plannedX=" + format(decks.plannedX())
                        + ", plannedZ=" + format(decks.plannedZ())
                        + ", plannedGapBlocks="
                        + String.format(Locale.ROOT, "%.1f", gapHalfBlocks / 2.0),
                "requirements: loopsAllowed=" + site.loopRampsAllowed()
                        + ", allMovementsFreeFlow="
                        + site.requireAllMovementsFreeFlow());
    }

    private static String format(HalfBlockElevation elevation) {
        return String.format(Locale.ROOT, "%.1f", elevation.halfBlocks() / 2.0);
    }

    public enum Outcome {
        SELECTED,
        REJECTED,
        CONFLICTED,
        NO_CROSSING
    }
}
