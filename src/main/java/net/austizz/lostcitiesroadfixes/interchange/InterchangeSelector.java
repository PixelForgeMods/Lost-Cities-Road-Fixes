package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.planning.elevation.GradeProfilePlanner;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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

        Map<InterchangeDesignId, InterchangeDesign> byId = new HashMap<>();
        for (InterchangeDesign design : designs) {
            InterchangeDesign previous = byId.put(design.id(), design);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate interchange design " + design.id());
            }
        }
        this.designs = byId.values().stream()
                .sorted(Comparator.comparingInt((InterchangeDesign design) -> design.type().ordinal())
                        .thenComparing(InterchangeDesign::id))
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
                                tieKey(site.selectionSeed(), left.design()),
                                tieKey(site.selectionSeed(), right.design())))
                        .thenComparing(evaluation -> evaluation.design().id()))
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
        int turningRouteRun = InterchangeRouteMetrics.shortestTurningRouteRunBlocks(
                design, site.approachRunBlocks());
        if (gradeRun > turningRouteRun) {
            reasons.add("turning-ramp grade requires " + gradeRun
                    + " blocks but the shortest route provides " + turningRouteRun);
        }
        if (design.type() == InterchangeType.CLOVERLEAF) {
            double loopRadius = StrictMath.max(
                    standard.minimumCurveRadiusBlocks(
                            net.austizz.lostcitiesroadfixes.road.RoadKind.RAMP),
                    StrictMath.ceil(gradeRun / (StrictMath.PI * 1.5)));
            double outerRadius = design.minimumRadiusBlocks() - 8.0;
            if (outerRadius < loopRadius * 6.0) {
                reasons.add("loop grades cannot be separated from the outer ramps");
            }
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

    private static long tieKey(long seed, InterchangeDesign design) {
        long idHash = 0xcbf29ce484222325L;
        String id = design.id().toString();
        for (int index = 0; index < id.length(); index++) {
            idHash ^= id.charAt(index);
            idHash *= 0x100000001b3L;
        }
        long value = seed ^ idHash;
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
