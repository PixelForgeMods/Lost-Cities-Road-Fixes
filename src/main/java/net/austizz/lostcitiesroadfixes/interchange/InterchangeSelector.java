package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.planning.elevation.GradeProfilePlanner;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InterchangeSelector {
    private final List<InterchangeDesign> designs;
    private final GradeProfilePlanner gradePlanner;
    private final RoadDesignStandard standard;

    public InterchangeSelector(List<InterchangeDesign> designs, RoadDesignStandard standard) {
        Objects.requireNonNull(designs, "designs");
        this.standard = Objects.requireNonNull(standard, "standard");
        this.gradePlanner = new GradeProfilePlanner(standard);

        Map<InterchangeType, InterchangeDesign> byType = new EnumMap<>(InterchangeType.class);
        for (InterchangeDesign design : designs) {
            InterchangeDesign previous = byType.put(design.type(), design);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate interchange design " + design.type());
            }
        }
        this.designs = List.of(InterchangeType.values()).stream()
                .map(byType::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public static InterchangeSelector withBuiltIns() {
        return new InterchangeSelector(InterchangeCatalogue.builtIns(), RoadDesignStandard.DEFAULT);
    }

    public InterchangeDecision select(InterchangeSite site) {
        Objects.requireNonNull(site, "site");
        List<InterchangeEvaluation> evaluations = designs.stream()
                .map(design -> evaluate(design, site))
                .toList();

        Optional<InterchangeDesign> selected = evaluations.stream()
                .filter(InterchangeEvaluation::feasible)
                .min(Comparator
                        .comparingInt((InterchangeEvaluation evaluation) ->
                                evaluation.score().orElseThrow())
                        .thenComparing((left, right) -> Long.compareUnsigned(
                                tieKey(site.selectionSeed(), left.design().type()),
                                tieKey(site.selectionSeed(), right.design().type())))
                        .thenComparingInt(evaluation -> evaluation.design().type().ordinal()))
                .map(InterchangeEvaluation::design);

        return new InterchangeDecision(site, selected, evaluations);
    }

    private InterchangeEvaluation evaluate(InterchangeDesign design, InterchangeSite site) {
        List<String> reasons = new ArrayList<>();
        if (design.form() != site.form()) {
            reasons.add("requires a " + design.form() + " junction");
        }
        if (design.minimumRadiusBlocks() > site.availableRadiusBlocks()) {
            reasons.add("radius requires " + design.minimumRadiusBlocks() + " blocks");
        }
        if (design.requiredQuadrants() > site.availableQuadrants()) {
            reasons.add("footprint requires " + design.requiredQuadrants() + " quadrants");
        }
        if (design.minimumApproachRunBlocks() > site.approachRunBlocks()) {
            reasons.add("design requires " + design.minimumApproachRunBlocks() + " approach blocks");
        }

        int gradeRun = gradePlanner.minimumRunBlocks(site.lowerDeck(), site.upperDeck());
        if (gradeRun > site.approachRunBlocks()) {
            reasons.add("grade requires " + gradeRun + " approach blocks");
        }

        int separationHalfBlocks = site.upperDeck().halfBlocks() - site.lowerDeck().halfBlocks();
        int minimumClearanceHalfBlocks = standard.minimumVehicleClearanceBlocks() * 2;
        if (separationHalfBlocks < minimumClearanceHalfBlocks) {
            reasons.add("deck separation provides less than "
                    + standard.minimumVehicleClearanceBlocks() + " blocks of clearance");
        }
        if (!design.capacity().supports(site.demand())) {
            reasons.add("capacity " + design.capacity() + " is below " + site.demand() + " demand");
        }
        if (design.structureLevels() > site.maximumStructureLevels()) {
            reasons.add("requires " + design.structureLevels() + " structure levels");
        }
        if (design.usesLoopRamps() && !site.loopRampsAllowed()) {
            reasons.add("requires loop-ramp space");
        }
        if (site.requireAllMovementsFreeFlow() && !design.allMovementsFreeFlow()) {
            reasons.add("does not provide all free-flow movements");
        }

        return reasons.isEmpty()
                ? InterchangeEvaluation.feasible(design, score(design, site))
                : InterchangeEvaluation.rejected(design, reasons);
    }

    private static int score(InterchangeDesign design, InterchangeSite site) {
        int capacitySlack = design.capacity().excessCapacityOver(site.demand());
        int quadrantSlack = site.availableQuadrants() - design.requiredQuadrants();
        int radiusSlack = site.availableRadiusBlocks() - design.minimumRadiusBlocks();
        int levelSlack = site.maximumStructureLevels() - design.structureLevels();

        return capacitySlack * 10_000
                + quadrantSlack * 1_000
                + radiusSlack * 10
                + levelSlack * 100
                + design.constructionComplexity() * 50;
    }

    private static long tieKey(long seed, InterchangeType type) {
        long value = seed ^ (0x9e3779b97f4a7c15L * (type.ordinal() + 1L));
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
