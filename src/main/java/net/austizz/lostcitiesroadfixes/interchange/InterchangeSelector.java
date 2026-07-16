package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.planning.elevation.GradeProfilePlanner;
import net.austizz.lostcitiesroadfixes.interchange.geometry.PlanarPoint;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeGeometrySite;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeLayoutFactory;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InterchangeSelector {
    private final List<InterchangeDesign> designs;
    private final GradeProfilePlanner gradePlanner;
    private final InterchangeLayoutFactory layoutFactory;
    private final RoadDesignStandard standard;
    private final ConcurrentMap<CompilationKey, CandidateCompilation> compilationCache =
            new ConcurrentHashMap<>();

    public InterchangeSelector(List<InterchangeDesign> designs, RoadDesignStandard standard) {
        Objects.requireNonNull(designs, "designs");
        this.standard = Objects.requireNonNull(standard, "standard");
        this.gradePlanner = new GradeProfilePlanner(standard);
        this.layoutFactory = new InterchangeLayoutFactory(standard);

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
                        .thenComparing(evaluation -> evaluation.design().id()))
                .map(InterchangeEvaluation::design);

        return new InterchangeDecision(site, selected, evaluations);
    }

    private InterchangeEvaluation evaluate(InterchangeDesign design, InterchangeSite site) {
        List<String> reasons = new ArrayList<>();
        int compiledApproach = -1;
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
        int physicalLevelSpan = Math.multiplyExact(
                design.structureLevels() - 1, minimumClearanceHalfBlocks);
        if (separationHalfBlocks < physicalLevelSpan) {
            reasons.add(design.type() == InterchangeType.STACK
                    ? "four physical levels require " + physicalLevelSpan / 2.0
                            + " blocks between the mainline decks"
                    : design.structureLevels() + " physical levels require "
                            + physicalLevelSpan / 2.0 + " blocks between the mainline decks");
        }
        if (design.usesLoopRamps() && !site.loopRampsAllowed()) {
            reasons.add("requires loop-ramp space");
        }
        if (site.requireAllMovementsFreeFlow() && !design.allMovementsFreeFlow()) {
            reasons.add("does not provide all free-flow movements");
        }

        if (reasons.isEmpty()) {
            CandidateCompilation compilation = compile(design, site);
            if (!compilation.feasible()) {
                reasons.add("geometry rejected: " + compilation.failure());
            } else {
                compiledApproach = compilation.approachRunBlocks();
            }
        }

        if (!reasons.isEmpty()) {
            return InterchangeEvaluation.rejected(design, reasons);
        }
        int footprintRadius = Math.addExact(
                design.minimumRadiusBlocks(),
                Math.floorDiv(standard.arterialCrossSection().totalWidthBlocks(), 2));
        int displacedBuildings = site.environment().displacedBuildings(footprintRadius);
        return InterchangeEvaluation.feasible(
                design,
                score(design, site, compiledApproach, displacedBuildings),
                compiledApproach,
                displacedBuildings);
    }

    private CandidateCompilation compile(InterchangeDesign design, InterchangeSite site) {
        CompilationKey key = new CompilationKey(
                design,
                site.approachRunBlocks(),
                site.xRoadNativeElevation(),
                site.zRoadNativeElevation(),
                site.xRoadCenterElevation(),
                site.zRoadCenterElevation());
        return compilationCache.computeIfAbsent(
                key, ignored -> compileUncached(design, site));
    }

    private CandidateCompilation compileUncached(
            InterchangeDesign design,
            InterchangeSite site) {
        int firstApproach = roundUpToChunk(design.minimumApproachRunBlocks());
        String lastFailure = "no chunk-aligned approach length is available";
        for (int approach = firstApproach;
                approach <= site.approachRunBlocks();
                approach = Math.addExact(approach, 16)) {
            try {
                layoutFactory.create(design, new InterchangeGeometrySite(
                        new PlanarPoint(0.0, 0.0),
                        approach,
                        site.xRoadNativeElevation(),
                        site.zRoadNativeElevation(),
                        site.xRoadCenterElevation(),
                        site.zRoadCenterElevation()));
                return CandidateCompilation.feasible(approach);
            } catch (IllegalArgumentException exception) {
                lastFailure = exception.getMessage();
            }
        }
        return CandidateCompilation.rejected(lastFailure);
    }

    private static int roundUpToChunk(int blocks) {
        return Math.multiplyExact(Math.floorDiv(Math.addExact(blocks, 15), 16), 16);
    }

    private static int score(
            InterchangeDesign design,
            InterchangeSite site,
            int compiledApproach,
            int displacedBuildings) {
        int capacitySlack = design.capacity().excessCapacityOver(site.demand());
        int quadrantSlack = site.availableQuadrants() - design.requiredQuadrants();
        int operationalPenalty = site.demand() == TrafficDemand.HIGH
                && !design.allMovementsFreeFlow() ? 20_000 : 0;
        int verticalWork = design.structureLevels() - 1;

        return Math.addExact(
                Math.multiplyExact(displacedBuildings, 1_000_000),
                capacitySlack * 100_000
                        + quadrantSlack * 5_000
                        + operationalPenalty
                        + verticalWork * 1_000
                        + design.minimumRadiusBlocks() * 10
                        + compiledApproach
                        + design.constructionComplexity() * 50);
    }

    private record CandidateCompilation(int approachRunBlocks, String failure) {
        private static CandidateCompilation feasible(int approachRunBlocks) {
            return new CandidateCompilation(approachRunBlocks, null);
        }

        private static CandidateCompilation rejected(String failure) {
            return new CandidateCompilation(-1, Objects.requireNonNull(failure, "failure"));
        }

        private boolean feasible() {
            return failure == null;
        }
    }

    private record CompilationKey(
            InterchangeDesign design,
            int maximumApproachRunBlocks,
            HalfBlockElevation xRoadNativeElevation,
            HalfBlockElevation zRoadNativeElevation,
            HalfBlockElevation xRoadCenterElevation,
            HalfBlockElevation zRoadCenterElevation) {
    }

}
