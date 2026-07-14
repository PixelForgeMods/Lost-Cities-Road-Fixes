package net.austizz.lostcitiesroadfixes.interchange.layout;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesign;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeRouteMetrics;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeType;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampCenterline;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampElevationKeyframe;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampPathBuilder;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampRoute;
import net.austizz.lostcitiesroadfixes.planning.elevation.GradeProfilePlanner;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class InterchangeLayoutFactory {
    private static final int RAMP_WIDTH_BLOCKS = 8;
    private static final List<ApproachDirection> CLOCKWISE_APPROACHES = List.of(
            ApproachDirection.WEST,
            ApproachDirection.NORTH,
            ApproachDirection.EAST,
            ApproachDirection.SOUTH);

    private final RoadDesignStandard standard;
    private final GradeProfilePlanner gradePlanner;

    public InterchangeLayoutFactory(RoadDesignStandard standard) {
        this.standard = Objects.requireNonNull(standard, "standard");
        this.gradePlanner = new GradeProfilePlanner(standard);
    }

    public InterchangeLayout create(
            InterchangeDesign design,
            InterchangeGeometrySite site) {
        Objects.requireNonNull(design, "design");
        return create(design, site, defaultApproaches(design.form()));
    }

    public InterchangeLayout create(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            Set<ApproachDirection> approaches) {
        Objects.requireNonNull(design, "design");
        Objects.requireNonNull(site, "site");
        Objects.requireNonNull(approaches, "approaches");
        if (site.approachRunBlocks() < design.minimumApproachRunBlocks()) {
            throw new IllegalArgumentException(
                    design.id() + " requires " + design.minimumApproachRunBlocks()
                            + " approach blocks but the site has " + site.approachRunBlocks());
        }
        if (site.approachRunBlocks() <= design.minimumRadiusBlocks()) {
            throw new IllegalArgumentException(
                    "Approach run must extend beyond the interchange radius for " + design.id());
        }

        Set<ApproachDirection> surveyedApproaches = EnumSet.copyOf(approaches);
        int expectedApproaches = design.form() == JunctionForm.THREE_WAY ? 3 : 4;
        if (surveyedApproaches.size() != expectedApproaches) {
            throw new IllegalArgumentException(
                    design.form() + " layout requires " + expectedApproaches + " approaches");
        }
        ApproachDirection missingApproach = design.form() == JunctionForm.THREE_WAY
                ? EnumSet.complementOf(EnumSet.copyOf(surveyedApproaches)).iterator().next()
                : null;
        List<InterchangeConnection> connections = design.geometry()
                .map(geometry -> createBlueprintConnections(
                        design, site, missingApproach, geometry))
                .orElseGet(() -> finishConnections(design, createDrafts(
                        design, site, surveyedApproaches, missingApproach)));
        return new InterchangeLayout(design, site, surveyedApproaches, connections);
    }

    private List<InterchangeConnection> createBlueprintConnections(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            ApproachDirection missingApproach,
            InterchangeGeometryBlueprint geometry) {
        List<InterchangeConnection> result = new ArrayList<>(geometry.movements().size());
        for (InterchangeMovementBlueprint blueprint : geometry.movements()) {
            InterchangeMovement movement = rotate(
                    blueprint.movement(), missingApproach);
            RampRoute route = switch (blueprint.form()) {
                case MAINLINE -> mainline(site, movement, blueprint.widthBlocks());
                case DIRECT -> directTurn(
                        design, site, movement, blueprint.widthBlocks());
                case LOOP -> loopTurn(
                        design, site, movement, blueprint.widthBlocks());
            };
            result.add(new InterchangeConnection(
                    movement,
                    route,
                    blueprint.form(),
                    blueprint.control(),
                    blueprint.structureLevel()));
        }
        return List.copyOf(result);
    }

    private List<ConnectionDraft> createDrafts(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            Set<ApproachDirection> approaches,
            ApproachDirection missingApproach) {
        List<ConnectionDraft> result = new ArrayList<>();
        for (ApproachDirection from : CLOCKWISE_APPROACHES) {
            if (!approaches.contains(from) || !approaches.contains(from.opposite())) {
                continue;
            }
            InterchangeMovement movement = new InterchangeMovement(
                    from, from.opposite(), MovementKind.STRAIGHT);
            result.add(new ConnectionDraft(
                    movement,
                    mainline(site, movement, RAMP_WIDTH_BLOCKS),
                    RampForm.MAINLINE));
        }
        for (ApproachDirection from : CLOCKWISE_APPROACHES) {
            if (!approaches.contains(from)) {
                continue;
            }
            addTurn(result, design, site, approaches, missingApproach, from, MovementKind.RIGHT);
            addTurn(result, design, site, approaches, missingApproach, from, MovementKind.LEFT);
        }
        return result;
    }

    private void addTurn(
            List<ConnectionDraft> result,
            InterchangeDesign design,
            InterchangeGeometrySite site,
            Set<ApproachDirection> approaches,
            ApproachDirection missingApproach,
            ApproachDirection from,
            MovementKind kind) {
        ApproachDirection destination = kind == MovementKind.RIGHT
                ? from.rightTurnDestination()
                : from.leftTurnDestination();
        if (!approaches.contains(destination)) {
            return;
        }
        InterchangeMovement movement = new InterchangeMovement(from, destination, kind);
        boolean loop = kind == MovementKind.LEFT
                && usesLoop(design.type(), canonicalDirection(from, missingApproach));
        result.add(new ConnectionDraft(
                movement,
                loop
                        ? loopTurn(design, site, movement, RAMP_WIDTH_BLOCKS)
                        : directTurn(design, site, movement, RAMP_WIDTH_BLOCKS),
                loop ? RampForm.LOOP : RampForm.DIRECT));
    }

    private RampRoute mainline(
            InterchangeGeometrySite site,
            InterchangeMovement movement,
            int widthBlocks) {
        InterchangePort start = site.port(movement.from(), TrafficFlow.INBOUND);
        InterchangePort end = site.port(movement.to(), TrafficFlow.OUTBOUND);
        RampPathBuilder builder = new RampPathBuilder(standard, start.point(), start.heading())
                .straight(site.approachRunBlocks() * 2.0);
        RampCenterline centerline = builder.build(List.of(
                new RampElevationKeyframe(0.0, start.elevation()),
                new RampElevationKeyframe(
                        site.approachRunBlocks(),
                        site.centerElevation(movement.from())),
                new RampElevationKeyframe(builder.lengthBlocks(), end.elevation())));
        return new RampRoute(centerline, widthBlocks);
    }

    private RampRoute directTurn(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            InterchangeMovement movement,
            int widthBlocks) {
        double tangent = InterchangeRouteMetrics.directTangentBlocks(
                design, site.approachRunBlocks());
        double radius = InterchangeRouteMetrics.directRadiusBlocks(
                design, movement.kind());
        InterchangePort start = site.port(movement.from(), TrafficFlow.INBOUND);
        InterchangePort end = site.port(movement.to(), TrafficFlow.OUTBOUND);
        RampPathBuilder builder = new RampPathBuilder(standard, start.point(), start.heading())
                .straight(tangent);
        if (movement.kind() == MovementKind.RIGHT) {
            builder.turnRight(radius, 90.0);
        } else {
            builder.turnLeft(radius, 90.0);
        }
        RampCenterline centerline = turningCenterline(
                builder.straight(tangent),
                start.elevation(),
                end.elevation(),
                tangent);
        requireEndpoint(design, movement, end, centerline);
        return new RampRoute(centerline, widthBlocks);
    }

    private RampRoute loopTurn(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            InterchangeMovement movement,
            int widthBlocks) {
        double loopRadius = InterchangeRouteMetrics.loopRadiusBlocks(design);
        double tangent = InterchangeRouteMetrics.loopTangentBlocks(
                design, site.approachRunBlocks());
        InterchangePort start = site.port(movement.from(), TrafficFlow.INBOUND);
        InterchangePort end = site.port(movement.to(), TrafficFlow.OUTBOUND);
        RampPathBuilder builder = new RampPathBuilder(standard, start.point(), start.heading())
                .straight(tangent)
                .turnRight(loopRadius, 270.0)
                .straight(tangent);
        RampCenterline centerline = turningCenterline(
                builder,
                start.elevation(),
                end.elevation(),
                tangent);
        requireEndpoint(design, movement, end, centerline);
        return new RampRoute(centerline, widthBlocks);
    }

    private RampCenterline turningCenterline(
            RampPathBuilder builder,
            HalfBlockElevation start,
            HalfBlockElevation end,
            double finalMergeTangentBlocks) {
        if (start.equals(end)) {
            return builder.build(start, end);
        }
        int gradeRun = gradePlanner.minimumRunBlocks(start, end);
        double preMergeRun = builder.lengthBlocks() - finalMergeTangentBlocks;
        if (gradeRun > preMergeRun + 1.0e-9) {
            throw new IllegalArgumentException(
                    "Turning ramp grade requires " + gradeRun
                            + " blocks before its merge but only "
                            + String.format(java.util.Locale.ROOT, "%.3f", preMergeRun)
                            + " are available");
        }
        return builder.build(List.of(
                new RampElevationKeyframe(0.0, start),
                new RampElevationKeyframe(gradeRun, end),
                new RampElevationKeyframe(builder.lengthBlocks(), end)));
    }

    private static InterchangeMovement rotate(
            InterchangeMovement canonical,
            ApproachDirection missingApproach) {
        if (missingApproach == null) {
            return canonical;
        }
        ApproachDirection from = rotate(canonical.from(), missingApproach.ordinal());
        ApproachDirection to = rotate(canonical.to(), missingApproach.ordinal());
        return new InterchangeMovement(from, to, canonical.kind());
    }

    private static ApproachDirection rotate(
            ApproachDirection direction,
            int clockwiseQuarterTurns) {
        ApproachDirection[] directions = ApproachDirection.values();
        return directions[Math.floorMod(
                direction.ordinal() + clockwiseQuarterTurns,
                directions.length)];
    }

    private static List<InterchangeConnection> finishConnections(
            InterchangeDesign design,
            List<ConnectionDraft> drafts) {
        if (design.freeFlowMovementCount() > drafts.size()) {
            throw new IllegalArgumentException(
                    design.id() + " declares " + design.freeFlowMovementCount()
                            + " free-flow movements but has only " + drafts.size() + " routes");
        }

        List<Integer> priority = new ArrayList<>();
        for (int index = 0; index < drafts.size(); index++) {
            priority.add(index);
        }
        priority.sort(Comparator
                .comparingInt((Integer index) -> freeFlowPriority(drafts.get(index)))
                .thenComparingInt(Integer::intValue));
        Set<Integer> freeFlow = new HashSet<>(
                priority.subList(0, design.freeFlowMovementCount()));

        List<InterchangeConnection> result = new ArrayList<>(drafts.size());
        for (int index = 0; index < drafts.size(); index++) {
            ConnectionDraft draft = drafts.get(index);
            RampControl control = freeFlow.contains(index)
                    ? RampControl.FREE_FLOW
                    : controlled(design.type(), draft.movement());
            int structureLevel = 1 + index % design.structureLevels();
            result.add(new InterchangeConnection(
                    draft.movement(),
                    draft.route(),
                    draft.form(),
                    control,
                    structureLevel));
        }
        return List.copyOf(result);
    }

    private static int freeFlowPriority(ConnectionDraft draft) {
        if (draft.form() == RampForm.MAINLINE) {
            return 0;
        }
        if (draft.form() == RampForm.LOOP) {
            return 1;
        }
        return draft.movement().kind() == MovementKind.RIGHT ? 2 : 3;
    }

    private static RampControl controlled(
            InterchangeType type,
            InterchangeMovement movement) {
        if (type == InterchangeType.SPUI || movement.kind() == MovementKind.LEFT) {
            return RampControl.SIGNALIZED;
        }
        return RampControl.YIELD;
    }

    private static boolean usesLoop(InterchangeType type, ApproachDirection from) {
        return switch (type) {
            case TRUMPET -> from == ApproachDirection.EAST;
            case PARTIAL_CLOVERLEAF ->
                    from == ApproachDirection.WEST || from == ApproachDirection.EAST;
            case CLOVERLEAF -> true;
            default -> false;
        };
    }

    private static ApproachDirection canonicalDirection(
            ApproachDirection direction,
            ApproachDirection missingApproach) {
        if (missingApproach == null) {
            return direction;
        }
        ApproachDirection[] directions = ApproachDirection.values();
        return directions[Math.floorMod(
                direction.ordinal() - missingApproach.ordinal(), directions.length)];
    }

    private static Set<ApproachDirection> defaultApproaches(JunctionForm form) {
        return form == JunctionForm.THREE_WAY
                ? EnumSet.of(
                        ApproachDirection.WEST,
                        ApproachDirection.EAST,
                        ApproachDirection.SOUTH)
                : EnumSet.allOf(ApproachDirection.class);
    }

    private static void requireEndpoint(
            InterchangeDesign design,
            InterchangeMovement movement,
            InterchangePort expected,
            RampCenterline centerline) {
        double deltaX = StrictMath.abs(
                centerline.endPose().point().x() - expected.point().x());
        double deltaZ = StrictMath.abs(
                centerline.endPose().point().z() - expected.point().z());
        if (deltaX > 1.0e-7 || deltaZ > 1.0e-7) {
            throw new IllegalStateException(
                    design.id() + " route " + movement + " misses its port by "
                            + deltaX + ", " + deltaZ);
        }
    }

    private record ConnectionDraft(
            InterchangeMovement movement,
            RampRoute route,
            RampForm form) {
    }
}
