package net.austizz.lostcitiesroadfixes.interchange.layout;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesign;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeRouteMetrics;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeType;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.geometry.PlanarPoint;
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
    private static final int AUXILIARY_TAPER_FORWARD_BLOCKS = 32;
    private static final int TERMINAL_SEPARATION_BLOCKS = 16;
    private static final double AUXILIARY_LATERAL_SHIFT_BLOCKS = 8.0;
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
        if (design.geometry().isPresent()) {
            List<InterchangeConnection> connections = createBlueprintConnections(
                    design, site, missingApproach, design.geometry().orElseThrow());
            return new InterchangeLayout(
                    design, site, surveyedApproaches, connections, List.of());
        }
        List<InterchangeConnection> connections = finishConnections(
                design,
                site,
                createDrafts(design, site, surveyedApproaches, missingApproach));
        return new InterchangeLayout(
                design,
                site,
                surveyedApproaches,
                connections,
                createAuxiliaryLanes(site, connections));
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
                        ? builtInLoopTurn(design, site, movement, RAMP_WIDTH_BLOCKS)
                        : builtInDirectTurn(design, site, movement, RAMP_WIDTH_BLOCKS),
                loop ? RampForm.LOOP : RampForm.DIRECT));
    }

    private List<InterchangeAuxiliaryLane> createAuxiliaryLanes(
            InterchangeGeometrySite site,
            List<InterchangeConnection> connections) {
        return connections.stream()
                .filter(connection -> connection.form() == RampForm.MAINLINE)
                .map(connection -> new InterchangeAuxiliaryLane(
                        connection.movement(),
                        auxiliaryLane(site, connection.movement())))
                .toList();
    }

    private RampRoute auxiliaryLane(
            InterchangeGeometrySite site,
            InterchangeMovement movement) {
        InterchangePort start = site.outerThroughPort(
                movement.from(), TrafficFlow.INBOUND);
        InterchangePort end = site.outerThroughPort(
                movement.to(), TrafficFlow.OUTBOUND);
        RampPathBuilder builder = new RampPathBuilder(
                standard, start.point(), start.heading());
        appendLateralShift(builder, true);
        double taperLength = builder.lengthBlocks();
        builder.straight(site.approachRunBlocks() * 2.0
                - AUXILIARY_TAPER_FORWARD_BLOCKS * 2.0);
        appendLateralShift(builder, false);
        RampCenterline centerline = builder.build(List.of(
                new RampElevationKeyframe(0.0, start.elevation()),
                new RampElevationKeyframe(
                        builder.lengthBlocks() / 2.0,
                        site.centerElevation(movement.from())),
                new RampElevationKeyframe(builder.lengthBlocks(), end.elevation())));
        if (taperLength <= AUXILIARY_TAPER_FORWARD_BLOCKS) {
            throw new IllegalStateException("Auxiliary taper has no positive curved length");
        }
        return new RampRoute(centerline, RAMP_WIDTH_BLOCKS);
    }

    private double appendLateralShift(RampPathBuilder builder, boolean outward) {
        double startLength = builder.lengthBlocks();
        double angleRadians = 2.0 * StrictMath.atan(
                AUXILIARY_LATERAL_SHIFT_BLOCKS / AUXILIARY_TAPER_FORWARD_BLOCKS);
        double radius = AUXILIARY_TAPER_FORWARD_BLOCKS
                / (2.0 * StrictMath.sin(angleRadians));
        double degrees = StrictMath.toDegrees(angleRadians);
        if (outward) {
            builder.turnRight(radius, degrees).turnLeft(radius, degrees);
        } else {
            builder.turnLeft(radius, degrees).turnRight(radius, degrees);
        }
        return builder.lengthBlocks() - startLength;
    }

    private RampRoute builtInDirectTurn(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            InterchangeMovement movement,
            int widthBlocks) {
        int terminalDistance = terminalDistance(design, site, movement.kind());
        double radius = builtInDirectRadius(design, movement.kind());
        InterchangePort start = site.auxiliaryPort(
                movement.from(), TrafficFlow.INBOUND, terminalDistance);
        InterchangePort end = site.auxiliaryPort(
                movement.to(), TrafficFlow.OUTBOUND, terminalDistance);
        int turnDirection = movement.kind() == MovementKind.RIGHT ? 1 : -1;
        TangentLengths tangents = solveShiftedTangents(
                design, movement, start, end, radius, 90.0, turnDirection);
        RampPathBuilder builder = new RampPathBuilder(
                standard, start.point(), start.heading());
        double departureShift = appendLateralShift(builder, true);
        builder.straight(tangents.departureBlocks());
        if (turnDirection > 0) {
            builder.turnRight(radius, 90.0);
        } else {
            builder.turnLeft(radius, 90.0);
        }
        builder.straight(tangents.arrivalBlocks());
        double arrivalShift = appendLateralShift(builder, false);
        RampCenterline centerline = separatedTurningCenterline(
                design,
                builder,
                start.elevation(),
                end.elevation(),
                departureShift,
                tangents.departureBlocks(),
                arrivalShift);
        requireEndpoint(design, movement, end, centerline);
        return new RampRoute(centerline, widthBlocks);
    }

    private RampRoute builtInLoopTurn(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            InterchangeMovement movement,
            int widthBlocks) {
        int terminalDistance = terminalDistance(design, site, movement.kind());
        InterchangePort start = site.auxiliaryPort(
                movement.from(), TrafficFlow.INBOUND, terminalDistance);
        InterchangePort end = site.auxiliaryPort(
                movement.to(), TrafficFlow.OUTBOUND, terminalDistance);
        int gradeRun = gradePlanner.minimumRunBlocks(
                start.elevation(), end.elevation());
        double radius = StrictMath.max(
                standard.minimumCurveRadiusBlocks(net.austizz.lostcitiesroadfixes.road.RoadKind.RAMP),
                StrictMath.ceil(gradeRun / (StrictMath.PI * 1.5)));
        if (design.type() == InterchangeType.CLOVERLEAF
                && builtInDirectRadius(design, MovementKind.RIGHT) < radius * 6.0) {
            throw new IllegalArgumentException(
                    design.id() + " cannot separate its loops from its outer ramps");
        }
        TangentLengths tangents = solveShiftedTangents(
                design, movement, start, end, radius, 270.0, 1);
        RampPathBuilder builder = new RampPathBuilder(
                standard, start.point(), start.heading());
        double departureShift = appendLateralShift(builder, true);
        builder.straight(tangents.departureBlocks())
                .turnRight(radius, 270.0)
                .straight(tangents.arrivalBlocks());
        double arrivalShift = appendLateralShift(builder, false);
        RampCenterline centerline = separatedTurningCenterline(
                design,
                builder,
                start.elevation(),
                end.elevation(),
                departureShift,
                tangents.departureBlocks(),
                arrivalShift);
        requireEndpoint(design, movement, end, centerline);
        return new RampRoute(centerline, widthBlocks);
    }

    private TangentLengths solveShiftedTangents(
            InterchangeDesign design,
            InterchangeMovement movement,
            InterchangePort start,
            InterchangePort end,
            double radius,
            double sweepDegrees,
            int turnDirection) {
        double startHeading = start.heading().radians();
        double endHeading = end.heading().radians();
        PlanarPoint departureShift = shiftDisplacement(startHeading, true);
        PlanarPoint arrivalShift = shiftDisplacement(endHeading, false);
        PlanarPoint arc = arcDisplacement(
                startHeading, radius, StrictMath.toRadians(sweepDegrees), turnDirection);
        double residualX = end.point().x() - start.point().x()
                - departureShift.x() - arc.x() - arrivalShift.x();
        double residualZ = end.point().z() - start.point().z()
                - departureShift.z() - arc.z() - arrivalShift.z();
        double startX = StrictMath.cos(startHeading);
        double startZ = StrictMath.sin(startHeading);
        double endX = StrictMath.cos(endHeading);
        double endZ = StrictMath.sin(endHeading);
        double determinant = startX * endZ - startZ * endX;
        double departure = (residualX * endZ - residualZ * endX) / determinant;
        double arrival = (startX * residualZ - startZ * residualX) / determinant;
        if (departure <= 0.0 || arrival <= 0.0) {
            throw new IllegalArgumentException(
                    design.id() + " has no separated terminal run for " + movement
                            + " (departure=" + departure + ", arrival=" + arrival + ')');
        }
        return new TangentLengths(departure, arrival);
    }

    private static PlanarPoint shiftDisplacement(
            double heading,
            boolean outward) {
        double lateral = outward
                ? AUXILIARY_LATERAL_SHIFT_BLOCKS
                : -AUXILIARY_LATERAL_SHIFT_BLOCKS;
        return new PlanarPoint(
                StrictMath.cos(heading) * AUXILIARY_TAPER_FORWARD_BLOCKS
                        - StrictMath.sin(heading) * lateral,
                StrictMath.sin(heading) * AUXILIARY_TAPER_FORWARD_BLOCKS
                        + StrictMath.cos(heading) * lateral);
    }

    private static PlanarPoint arcDisplacement(
            double heading,
            double radius,
            double sweepRadians,
            int direction) {
        double centerX = -direction * StrictMath.sin(heading) * radius;
        double centerZ = direction * StrictMath.cos(heading) * radius;
        double radialX = direction * StrictMath.sin(heading) * radius;
        double radialZ = -direction * StrictMath.cos(heading) * radius;
        double angle = direction * sweepRadians;
        double cosine = StrictMath.cos(angle);
        double sine = StrictMath.sin(angle);
        return new PlanarPoint(
                centerX + radialX * cosine - radialZ * sine,
                centerZ + radialX * sine + radialZ * cosine);
    }

    private RampCenterline separatedTurningCenterline(
            InterchangeDesign design,
            RampPathBuilder builder,
            HalfBlockElevation start,
            HalfBlockElevation end,
            double departureShift,
            double departureTangent,
            double arrivalShift) {
        if (start.equals(end)) {
            return builder.build(start, end);
        }
        int gradeRun = gradePlanner.minimumRunBlocks(start, end);
        boolean cloverleaf = design.type() == InterchangeType.CLOVERLEAF;
        double mergeStart = cloverleaf
                ? builder.lengthBlocks() - arrivalShift
                : builder.lengthBlocks() - 8.0;
        double gradeStart = cloverleaf
                ? departureShift + departureTangent
                : 8.0;
        double gradeEnd = gradeStart + gradeRun;
        if (gradeEnd > mergeStart + 1.0e-9) {
            throw new IllegalArgumentException(
                    "Separated turning ramp grade requires " + gradeRun
                            + " blocks but only " + (mergeStart - gradeStart)
                            + " are available");
        }
        return builder.build(List.of(
                new RampElevationKeyframe(0.0, start),
                new RampElevationKeyframe(gradeStart, start),
                new RampElevationKeyframe(gradeEnd, end),
                new RampElevationKeyframe(builder.lengthBlocks(), end)));
    }

    private int terminalDistance(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            MovementKind kind) {
        int distance = site.approachRunBlocks() - AUXILIARY_TAPER_FORWARD_BLOCKS;
        if (kind != MovementKind.LEFT) {
            return distance;
        }
        if (design.type() == InterchangeType.CLOVERLEAF) {
            double rightRadius = builtInDirectRadius(design, MovementKind.RIGHT);
            int afterOuterRampDiverge = (int) StrictMath.floor(
                    20.0 + rightRadius - TERMINAL_SEPARATION_BLOCKS);
            return StrictMath.min(
                    distance - TERMINAL_SEPARATION_BLOCKS,
                    afterOuterRampDiverge);
        }
        return distance - TERMINAL_SEPARATION_BLOCKS;
    }

    private double builtInDirectRadius(
            InterchangeDesign design,
            MovementKind kind) {
        double minimum = standard.minimumCurveRadiusBlocks(
                net.austizz.lostcitiesroadfixes.road.RoadKind.RAMP);
        return kind == MovementKind.RIGHT
                ? StrictMath.max(minimum, design.minimumRadiusBlocks()
                        - (design.type() == InterchangeType.CLOVERLEAF ? 8.0 : 32.0))
                : StrictMath.max(minimum, design.minimumRadiusBlocks() + 8.0);
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

    private List<InterchangeConnection> finishConnections(
            InterchangeDesign design,
            InterchangeGeometrySite site,
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
            RampRoute route = design.type() == InterchangeType.STACK
                    && draft.form() != RampForm.MAINLINE
                    ? stackTierRoute(site, draft.route(), structureLevel)
                    : draft.route();
            result.add(new InterchangeConnection(
                    draft.movement(),
                    route,
                    draft.form(),
                    control,
                    structureLevel));
        }
        return List.copyOf(result);
    }

    private RampRoute stackTierRoute(
            InterchangeGeometrySite site,
            RampRoute route,
            int structureLevel) {
        double firstCoreStation = Double.POSITIVE_INFINITY;
        double lastCoreStation = Double.NEGATIVE_INFINITY;
        for (var sample : route.centerline().samples()) {
            if (StrictMath.abs(sample.point().x() - site.center().x()) <= 24.0
                    && StrictMath.abs(sample.point().z() - site.center().z()) <= 24.0) {
                firstCoreStation = StrictMath.min(
                        firstCoreStation, sample.stationBlocks());
                lastCoreStation = StrictMath.max(
                        lastCoreStation, sample.stationBlocks());
            }
        }
        if (!Double.isFinite(firstCoreStation)) {
            return route;
        }
        HalfBlockElevation lower = site.xRoadCenterElevation().compareTo(
                site.zRoadCenterElevation()) <= 0
                ? site.xRoadCenterElevation() : site.zRoadCenterElevation();
        HalfBlockElevation upper = site.xRoadCenterElevation().compareTo(
                site.zRoadCenterElevation()) >= 0
                ? site.xRoadCenterElevation() : site.zRoadCenterElevation();
        int clearanceHalfBlocks = standard.minimumVehicleClearanceBlocks() * 2 + 2;
        HalfBlockElevation below = new HalfBlockElevation(
                lower.halfBlocks() - clearanceHalfBlocks);
        HalfBlockElevation above = new HalfBlockElevation(
                upper.halfBlocks() + clearanceHalfBlocks);
        HalfBlockElevation preferred = structureLevel % 2 == 0 ? above : below;
        HalfBlockElevation alternate = structureLevel % 2 == 0 ? below : above;
        HalfBlockElevation tier = fitsTier(
                route, firstCoreStation, lastCoreStation, preferred)
                ? preferred
                : fitsTier(route, firstCoreStation, lastCoreStation, alternate)
                        ? alternate
                        : null;
        if (tier == null) {
            throw new IllegalArgumentException(
                    "Stack ramp cannot reach a clear central structure tier"
                            + " (first=" + firstCoreStation
                            + ", remaining="
                            + (route.centerline().lengthBlocks() - lastCoreStation)
                            + ", belowRuns="
                            + gradePlanner.minimumRunBlocks(
                                    route.centerline().startElevation(), below)
                            + '/' + gradePlanner.minimumRunBlocks(
                                    below, route.centerline().endElevation())
                            + ", aboveRuns="
                            + gradePlanner.minimumRunBlocks(
                                    route.centerline().startElevation(), above)
                            + '/' + gradePlanner.minimumRunBlocks(
                                    above, route.centerline().endElevation()) + ')');
        }
        RampCenterline centerline = route.centerline().withElevationProfile(List.of(
                new RampElevationKeyframe(
                        0.0, route.centerline().startElevation()),
                new RampElevationKeyframe(firstCoreStation, tier),
                new RampElevationKeyframe(lastCoreStation, tier),
                new RampElevationKeyframe(
                        route.centerline().lengthBlocks(),
                        route.centerline().endElevation())));
        return new RampRoute(centerline, route.widthBlocks());
    }

    private boolean fitsTier(
            RampRoute route,
            double firstCoreStation,
            double lastCoreStation,
            HalfBlockElevation tier) {
        int departureRun = gradePlanner.minimumRunBlocks(
                route.centerline().startElevation(), tier);
        int arrivalRun = gradePlanner.minimumRunBlocks(
                tier, route.centerline().endElevation());
        return firstCoreStation + 1.0e-9 >= departureRun
                && route.centerline().lengthBlocks() - lastCoreStation + 1.0e-9
                        >= arrivalRun;
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

    private record TangentLengths(
            double departureBlocks,
            double arrivalBlocks) {
    }
}
