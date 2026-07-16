package net.austizz.lostcitiesroadfixes.interchange.layout;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesign;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeRouteMetrics;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeType;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.geometry.PlanarPoint;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampCenterline;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampCenterlineSample;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampElevationKeyframe;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampPathBuilder;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampRoute;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RoadHeading;
import net.austizz.lostcitiesroadfixes.planning.elevation.GradeProfilePlanner;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class InterchangeLayoutFactory {
    private static final int RAMP_WIDTH_BLOCKS = 10;
    private static final int AUXILIARY_TAPER_FORWARD_BLOCKS = 32;
    private static final int TERMINAL_GRADE_LOCK_BLOCKS = 40;
    private static final double AUXILIARY_LATERAL_SHIFT_BLOCKS = 9.0;
    private static final double RAMP_BRANCH_LATERAL_SHIFT_BLOCKS = 12.0;
    private static final double RAMP_GRADE_SEPARATION_BUFFER_BLOCKS = 4.0;
    private static final double RASTERIZED_SURFACE_MARGIN_BLOCKS = 1.5;
    private static final double CUSTOM_ROUTE_CLEARANCE_LOCK_BLOCKS = 52.0;
    private static final int CLOVERLEAF_LOOP_TANGENT_BLOCKS = 16;
    private static final int CLOVERLEAF_INNER_GRADE_LOCK_BLOCKS = 4;
    private static final double STACK_SHARED_TRUNK_BLOCKS = 288.0;
    private static final double STACK_BRANCH_ANGLE_DEGREES = 45.0;
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
        requirePhysicalStackLevels(design, site);

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
        List<InterchangeAuxiliaryLane> auxiliaryLanes = createAuxiliaryLanes(
                design, site, connections);
        connections = applyBuiltInStructureLevels(
                design, site, surveyedApproaches, connections, auxiliaryLanes);
        return new InterchangeLayout(
                design,
                site,
                surveyedApproaches,
                connections,
                auxiliaryLanes);
    }

    private List<InterchangeConnection> createBlueprintConnections(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            ApproachDirection missingApproach,
            InterchangeGeometryBlueprint geometry) {
        List<BlueprintRouteDraft> drafts = new ArrayList<>(geometry.movements().size());
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
            drafts.add(new BlueprintRouteDraft(movement, blueprint, route));
        }

        List<InterchangeConnection> result = new ArrayList<>(drafts.size());
        for (BlueprintRouteDraft draft : drafts) {
            InterchangeMovementBlueprint blueprint = draft.blueprint();
            RampRoute route = draft.route();
            route = applyBlueprintStructureLevel(
                    design,
                    site,
                    draft.movement(),
                    blueprint,
                    route,
                    customDepartureLock(draft, drafts),
                    customArrivalLock(draft, drafts));
            result.add(new InterchangeConnection(
                    draft.movement(),
                    route,
                    blueprint.form(),
                    blueprint.control(),
                    blueprint.structureLevel()));
        }
        requireCustomRouteClearance(design, result);
        return List.copyOf(result);
    }

    private RampRoute applyBlueprintStructureLevel(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            InterchangeMovement movement,
            InterchangeMovementBlueprint blueprint,
            RampRoute route,
            double departureLock,
            double arrivalLock) {
        HalfBlockElevation tier = structureLevelElevation(
                site, design.structureLevels(), blueprint.structureLevel());
        if (blueprint.form() == RampForm.MAINLINE) {
            // Mainline geometry is supplied by the surveyed arterial itself;
            // structure levels physically profile only authored ramp movements.
            return route;
        }

        RampCenterline centerline = route.centerline();
        HalfBlockElevation start = centerline.startElevation();
        HalfBlockElevation end = centerline.endElevation();
        int departureGradeRun = gradePlanner.minimumRunBlocks(start, tier);
        int arrivalGradeRun = gradePlanner.minimumRunBlocks(tier, end);
        double departureGradeStart = departureLock;
        double departureGradeEnd = departureGradeStart + departureGradeRun;
        double arrivalGradeEnd = centerline.lengthBlocks() - arrivalLock;
        double arrivalGradeStart = arrivalGradeEnd - arrivalGradeRun;
        double coreStation = centerline.lengthBlocks() / 2.0;
        if (departureGradeEnd > arrivalGradeStart + 1.0e-9
                || departureGradeEnd > coreStation + 1.0e-9
                || arrivalGradeStart < coreStation - 1.0e-9) {
            throw new IllegalArgumentException(
                    design.id() + " movement " + movement
                            + " cannot safely reach structure level "
                            + blueprint.structureLevel() + ": needs "
                            + (departureGradeRun + arrivalGradeRun)
                            + " graded blocks between terminal locks "
                            + String.format(
                                    java.util.Locale.ROOT,
                                    "%.3f/%.3f",
                                    departureLock,
                                    arrivalLock)
                            + " but route length is "
                            + String.format(
                                    java.util.Locale.ROOT,
                                    "%.3f",
                                    centerline.lengthBlocks()));
        }

        List<RampElevationKeyframe> profile = new ArrayList<>();
        appendKeyframe(profile, 0.0, start);
        appendKeyframe(profile, departureGradeStart, start);
        appendKeyframe(profile, departureGradeEnd, tier);
        appendKeyframe(profile, arrivalGradeStart, tier);
        appendKeyframe(profile, arrivalGradeEnd, end);
        appendKeyframe(profile, centerline.lengthBlocks(), end);
        return new RampRoute(
                centerline.withElevationProfile(profile),
                route.widthBlocks());
    }

    private static double customDepartureLock(
            BlueprintRouteDraft draft,
            List<BlueprintRouteDraft> drafts) {
        if (draft.blueprint().form() == RampForm.MAINLINE) {
            return TERMINAL_GRADE_LOCK_BLOCKS;
        }
        double lock = TERMINAL_GRADE_LOCK_BLOCKS;
        for (BlueprintRouteDraft candidate : drafts) {
            if (candidate.blueprint().form() == RampForm.MAINLINE
                    && candidate.movement().from() == draft.movement().from()) {
                lock = StrictMath.max(
                        lock,
                        sharedDepartureRun(draft.route(), candidate.route()).leftBlocks()
                                + CUSTOM_ROUTE_CLEARANCE_LOCK_BLOCKS);
            }
        }
        return lock;
    }

    private static double customArrivalLock(
            BlueprintRouteDraft draft,
            List<BlueprintRouteDraft> drafts) {
        if (draft.blueprint().form() == RampForm.MAINLINE) {
            return TERMINAL_GRADE_LOCK_BLOCKS;
        }
        double lock = TERMINAL_GRADE_LOCK_BLOCKS;
        for (BlueprintRouteDraft candidate : drafts) {
            if (candidate.blueprint().form() == RampForm.MAINLINE
                    && candidate.movement().to() == draft.movement().to()) {
                lock = StrictMath.max(
                        lock,
                        sharedArrivalRun(draft.route(), candidate.route()).leftBlocks()
                                + CUSTOM_ROUTE_CLEARANCE_LOCK_BLOCKS);
            }
        }
        return lock;
    }

    private static HalfBlockElevation structureLevelElevation(
            InterchangeGeometrySite site,
            int structureLevels,
            int structureLevel) {
        if (structureLevel < 1 || structureLevel > structureLevels) {
            throw new IllegalArgumentException(
                    "Structure level must be between 1 and " + structureLevels);
        }
        HalfBlockElevation lower = lowerCenterElevation(site);
        if (structureLevels == 1) {
            return lower;
        }
        HalfBlockElevation upper = upperCenterElevation(site);
        long span = (long) upper.halfBlocks() - lower.halfBlocks();
        long offset = Math.multiplyExact((long) structureLevel - 1L, span)
                / (structureLevels - 1L);
        return lower.plusHalfBlocks(Math.toIntExact(offset));
    }

    private void requireCustomRouteClearance(
            InterchangeDesign design,
            List<InterchangeConnection> connections) {
        int minimumSeparationHalfBlocks = Math.multiplyExact(
                standard.minimumVehicleClearanceBlocks(), 2);
        for (int leftIndex = 0; leftIndex < connections.size(); leftIndex++) {
            InterchangeConnection left = connections.get(leftIndex);
            for (int rightIndex = leftIndex + 1;
                    rightIndex < connections.size();
                    rightIndex++) {
                InterchangeConnection right = connections.get(rightIndex);
                double overlap = (left.route().widthBlocks()
                        + right.route().widthBlocks()) / 2.0 - 0.5;
                RouteSpatialIndex rightRoute = new RouteSpatialIndex(right.route());
                for (var leftSample : left.route().centerline().samples()) {
                    RouteProximity nearest = rightRoute.nearestWithin(
                            leftSample.point(), overlap);
                    if (nearest == null) {
                        continue;
                    }
                    int rightElevation = right.route().centerline()
                            .elevationAt(nearest.stationBlocks()).halfBlocks();
                    int separation = StrictMath.abs(
                            leftSample.elevation().halfBlocks() - rightElevation);
                    if (separation > 0
                            && separation < minimumSeparationHalfBlocks) {
                        throw new IllegalArgumentException(
                                design.id() + " has unsafe custom structure levels between "
                                        + left.movement() + " and " + right.movement()
                                        + " near station "
                                        + String.format(
                                                java.util.Locale.ROOT,
                                                "%.1f/%.1f",
                                                leftSample.stationBlocks(),
                                                nearest.stationBlocks())
                                        + ": elevations are "
                                        + leftSample.elevation().halfBlocks() + "/"
                                        + rightElevation + " half-blocks");
                    }
                }
            }
        }
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
                design.type() == InterchangeType.STACK
                        ? builtInStackTurn(design, site, movement, RAMP_WIDTH_BLOCKS)
                        : loop
                        ? builtInLoopTurn(design, site, movement, RAMP_WIDTH_BLOCKS)
                        : builtInDirectTurn(design, site, movement, RAMP_WIDTH_BLOCKS),
                loop ? RampForm.LOOP : RampForm.DIRECT));
    }

    private RampRoute builtInStackTurn(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            InterchangeMovement movement,
            int widthBlocks) {
        int terminalDistance = terminalDistance(design, site);
        InterchangePort start = site.auxiliaryPort(
                movement.from(), TrafficFlow.INBOUND, terminalDistance);
        InterchangePort end = site.auxiliaryPort(
                movement.to(), TrafficFlow.OUTBOUND, terminalDistance);
        int turnDirection = movement.kind() == MovementKind.RIGHT ? 1 : -1;
        double radius = builtInDirectRadius(design, movement.kind());

        double heading = start.heading().radians();
        double deltaX = end.point().x() - start.point().x();
        double deltaZ = end.point().z() - start.point().z();
        double forward = deltaX * StrictMath.cos(heading)
                + deltaZ * StrictMath.sin(heading);
        double turnSide = turnDirection * (
                -deltaX * StrictMath.sin(heading)
                        + deltaZ * StrictMath.cos(heading));
        if (StrictMath.abs(forward - turnSide) > 1.0e-6) {
            throw new IllegalArgumentException(
                    design.id() + " has asymmetric stack terminals for " + movement);
        }
        double diagonalProjection = forward - STACK_SHARED_TRUNK_BLOCKS - radius;
        if (diagonalProjection <= 0.0) {
            throw new IllegalArgumentException(
                    design.id() + " has no room to separate stack branches for " + movement);
        }
        double diagonal = diagonalProjection * StrictMath.sqrt(2.0);

        RampPathBuilder builder = new RampPathBuilder(
                standard, start.point(), start.heading())
                .straight(STACK_SHARED_TRUNK_BLOCKS);
        if (turnDirection > 0) {
            builder.turnRight(radius, STACK_BRANCH_ANGLE_DEGREES)
                    .straight(diagonal)
                    .turnRight(radius, STACK_BRANCH_ANGLE_DEGREES);
        } else {
            builder.turnLeft(radius, STACK_BRANCH_ANGLE_DEGREES)
                    .straight(diagonal)
                    .turnLeft(radius, STACK_BRANCH_ANGLE_DEGREES);
        }
        builder.straight(STACK_SHARED_TRUNK_BLOCKS);
        RampCenterline centerline = turningCenterline(
                builder,
                start.elevation(),
                end.elevation(),
                STACK_SHARED_TRUNK_BLOCKS);
        requireEndpoint(design, movement, end, centerline);
        return new RampRoute(centerline, widthBlocks);
    }

    private List<InterchangeAuxiliaryLane> createAuxiliaryLanes(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            List<InterchangeConnection> connections) {
        return connections.stream()
                .filter(connection -> connection.form() == RampForm.MAINLINE)
                .map(connection -> new InterchangeAuxiliaryLane(
                        connection.movement(),
                        auxiliaryLane(design, site, connection.movement())))
                .toList();
    }

    private RampRoute auxiliaryLane(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            InterchangeMovement movement) {
        InterchangePort start = site.outerThroughPort(
                movement.from(), TrafficFlow.INBOUND);
        InterchangePort end = site.outerThroughPort(
                movement.to(), TrafficFlow.OUTBOUND);
        RampPathBuilder builder = new RampPathBuilder(
                standard, start.point(), start.heading());
        appendLateralShift(builder, true, AUXILIARY_LATERAL_SHIFT_BLOCKS);
        double taperLength = builder.lengthBlocks();
        builder.straight(site.approachRunBlocks() * 2.0
                - AUXILIARY_TAPER_FORWARD_BLOCKS * 2.0);
        double arrivalTaperLength = appendLateralShift(
                builder, false, AUXILIARY_LATERAL_SHIFT_BLOCKS);
        int terminalDistance = terminalDistance(design, site);
        double terminalStraightOffset = site.approachRunBlocks()
                - terminalDistance - AUXILIARY_TAPER_FORWARD_BLOCKS;
        double departureTerminalStation = taperLength + terminalStraightOffset;
        double arrivalTerminalStation = builder.lengthBlocks()
                - arrivalTaperLength - terminalStraightOffset;
        List<RampElevationKeyframe> elevationProfile = new ArrayList<>();
        elevationProfile.add(new RampElevationKeyframe(0.0, start.elevation()));
        HalfBlockElevation departureTerminalElevation = site.auxiliaryPort(
                movement.from(),
                TrafficFlow.INBOUND,
                terminalDistance).elevation();
        elevationProfile.add(new RampElevationKeyframe(
                departureTerminalStation - 1.0, departureTerminalElevation));
        elevationProfile.add(new RampElevationKeyframe(
                departureTerminalStation + TERMINAL_GRADE_LOCK_BLOCKS,
                departureTerminalElevation));
        if (design.type() == InterchangeType.CLOVERLEAF) {
            int loopTerminalDistance = cloverleafLoopTerminalDistance(site);
            double innerStraightOffset = site.approachRunBlocks()
                    - loopTerminalDistance - AUXILIARY_TAPER_FORWARD_BLOCKS;
            double firstInnerStation = taperLength + innerStraightOffset;
            HalfBlockElevation firstInnerElevation = site.auxiliaryPort(
                    movement.from(),
                    TrafficFlow.INBOUND,
                    loopTerminalDistance).elevation();
            elevationProfile.add(new RampElevationKeyframe(
                    firstInnerStation - CLOVERLEAF_INNER_GRADE_LOCK_BLOCKS,
                    firstInnerElevation));
            elevationProfile.add(new RampElevationKeyframe(
                    firstInnerStation + CLOVERLEAF_INNER_GRADE_LOCK_BLOCKS,
                    firstInnerElevation));
        }
        elevationProfile.add(new RampElevationKeyframe(
                builder.lengthBlocks() / 2.0,
                site.centerElevation(movement.from())));
        if (design.type() == InterchangeType.CLOVERLEAF) {
            int loopTerminalDistance = cloverleafLoopTerminalDistance(site);
            double innerStraightOffset = site.approachRunBlocks()
                    - loopTerminalDistance - AUXILIARY_TAPER_FORWARD_BLOCKS;
            double secondInnerStation = builder.lengthBlocks()
                    - arrivalTaperLength - innerStraightOffset;
            HalfBlockElevation secondInnerElevation = site.auxiliaryPort(
                    movement.to(),
                    TrafficFlow.OUTBOUND,
                    loopTerminalDistance).elevation();
            elevationProfile.add(new RampElevationKeyframe(
                    secondInnerStation - CLOVERLEAF_INNER_GRADE_LOCK_BLOCKS,
                    secondInnerElevation));
            elevationProfile.add(new RampElevationKeyframe(
                    secondInnerStation + CLOVERLEAF_INNER_GRADE_LOCK_BLOCKS,
                    secondInnerElevation));
        }
        HalfBlockElevation arrivalTerminalElevation = site.auxiliaryPort(
                movement.to(),
                TrafficFlow.OUTBOUND,
                terminalDistance).elevation();
        elevationProfile.add(new RampElevationKeyframe(
                arrivalTerminalStation - TERMINAL_GRADE_LOCK_BLOCKS,
                arrivalTerminalElevation));
        elevationProfile.add(new RampElevationKeyframe(
                arrivalTerminalStation, arrivalTerminalElevation));
        elevationProfile.add(new RampElevationKeyframe(
                builder.lengthBlocks(), end.elevation()));
        RampCenterline centerline = builder.build(elevationProfile);
        if (taperLength <= AUXILIARY_TAPER_FORWARD_BLOCKS) {
            throw new IllegalStateException("Auxiliary taper has no positive curved length");
        }
        return new RampRoute(centerline, RAMP_WIDTH_BLOCKS);
    }

    private double appendLateralShift(
            RampPathBuilder builder,
            boolean outward,
            double lateralShiftBlocks) {
        double startLength = builder.lengthBlocks();
        double angleRadians = 2.0 * StrictMath.atan(
                lateralShiftBlocks / AUXILIARY_TAPER_FORWARD_BLOCKS);
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
        int terminalDistance = terminalDistance(design, site);
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
        double departureShift = appendLateralShift(
                builder, true, RAMP_BRANCH_LATERAL_SHIFT_BLOCKS);
        builder.straight(tangents.departureBlocks());
        if (turnDirection > 0) {
            builder.turnRight(radius, 90.0);
        } else {
            builder.turnLeft(radius, 90.0);
        }
        builder.straight(tangents.arrivalBlocks());
        double arrivalShift = appendLateralShift(
                builder, false, RAMP_BRANCH_LATERAL_SHIFT_BLOCKS);
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
        boolean fullCloverleaf = design.type() == InterchangeType.CLOVERLEAF;
        int terminalDistance = fullCloverleaf
                ? cloverleafLoopTerminalDistance(site)
                : terminalDistance(design, site);
        InterchangePort start = fullCloverleaf
                ? site.auxiliaryPort(
                        movement.from().opposite(),
                        TrafficFlow.OUTBOUND,
                        terminalDistance)
                : site.auxiliaryPort(
                        movement.from(), TrafficFlow.INBOUND, terminalDistance);
        InterchangePort end = fullCloverleaf
                ? site.auxiliaryPort(
                        movement.to().opposite(),
                        TrafficFlow.INBOUND,
                        terminalDistance)
                : site.auxiliaryPort(
                        movement.to(), TrafficFlow.OUTBOUND, terminalDistance);
        int gradeRun = gradePlanner.minimumRunBlocks(
                start.elevation(), end.elevation());
        double radius = fullCloverleaf
                ? cloverleafLoopRadius(site)
                : StrictMath.max(
                        standard.minimumCurveRadiusBlocks(
                                net.austizz.lostcitiesroadfixes.road.RoadKind.RAMP),
                        StrictMath.ceil(gradeRun / (StrictMath.PI * 1.5)));
        if (fullCloverleaf
                && builtInDirectRadius(design, MovementKind.RIGHT) < radius * 6.0) {
            throw new IllegalArgumentException(
                    design.id() + " cannot separate its loops from its outer ramps");
        }
        TangentLengths tangents = solveShiftedTangents(
                design, movement, start, end, radius, 270.0, 1);
        RampPathBuilder builder = new RampPathBuilder(
                standard, start.point(), start.heading());
        double departureShift = appendLateralShift(
                builder, true, RAMP_BRANCH_LATERAL_SHIFT_BLOCKS);
        builder.straight(tangents.departureBlocks())
                .turnRight(radius, 270.0)
                .straight(tangents.arrivalBlocks());
        double arrivalShift = appendLateralShift(
                builder, false, RAMP_BRANCH_LATERAL_SHIFT_BLOCKS);
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

    private double cloverleafLoopRadius(InterchangeGeometrySite site) {
        int gradeRun = gradePlanner.minimumRunBlocks(
                site.xRoadCenterElevation(), site.zRoadCenterElevation());
        return StrictMath.max(
                standard.minimumCurveRadiusBlocks(
                        net.austizz.lostcitiesroadfixes.road.RoadKind.RAMP),
                StrictMath.ceil(gradeRun / (StrictMath.PI * 1.5)));
    }

    private int cloverleafLoopTerminalDistance(InterchangeGeometrySite site) {
        return StrictMath.max(
                1,
                (int) StrictMath.floor(cloverleafLoopRadius(site))
                        - CLOVERLEAF_LOOP_TANGENT_BLOCKS);
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
                ? RAMP_BRANCH_LATERAL_SHIFT_BLOCKS
                : -RAMP_BRANCH_LATERAL_SHIFT_BLOCKS;
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
        double latestGradeEnd = cloverleaf
                ? builder.lengthBlocks() - arrivalShift
                : builder.lengthBlocks() - arrivalShift
                        - RAMP_GRADE_SEPARATION_BUFFER_BLOCKS;
        double earliestGradeStart = cloverleaf
                ? departureShift + departureTangent
                : departureShift + RAMP_GRADE_SEPARATION_BUFFER_BLOCKS;
        double gradeStart;
        double gradeEnd;
        if (start.compareTo(end) > 0) {
            gradeStart = earliestGradeStart;
            gradeEnd = gradeStart + gradeRun;
        } else {
            gradeEnd = latestGradeEnd;
            gradeStart = gradeEnd - gradeRun;
        }
        if (gradeStart + 1.0e-9 < earliestGradeStart
                || gradeEnd > latestGradeEnd + 1.0e-9) {
            throw new IllegalArgumentException(
                    "Separated turning ramp grade requires " + gradeRun
                            + " blocks but only "
                            + (latestGradeEnd - earliestGradeStart)
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
            InterchangeGeometrySite site) {
        int outerTerminal = site.approachRunBlocks()
                - AUXILIARY_TAPER_FORWARD_BLOCKS;
        if (design.type() != InterchangeType.STACK) {
            return outerTerminal;
        }
        int minimumClearanceHalfBlocks = standard.minimumVehicleClearanceBlocks() * 2;
        for (int inset = AUXILIARY_TAPER_FORWARD_BLOCKS;
                inset < site.approachRunBlocks();
                inset++) {
            int distance = site.approachRunBlocks() - inset;
            int xElevation = site.auxiliaryPort(
                    ApproachDirection.WEST,
                    TrafficFlow.INBOUND,
                    distance).elevation().halfBlocks();
            int zElevation = site.auxiliaryPort(
                    ApproachDirection.NORTH,
                    TrafficFlow.INBOUND,
                    distance).elevation().halfBlocks();
            if (StrictMath.abs(xElevation - zElevation)
                    >= minimumClearanceHalfBlocks) {
                return distance;
            }
        }
        throw new IllegalArgumentException(
                design.id() + " cannot reach vehicle clearance on its graded approaches");
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
        StackTierPlan stackTiers = design.type() == InterchangeType.STACK
                ? stackTierPlan(site, drafts)
                : null;

        List<InterchangeConnection> result = new ArrayList<>(drafts.size());
        for (int index = 0; index < drafts.size(); index++) {
            ConnectionDraft draft = drafts.get(index);
            RampControl control = freeFlow.contains(index)
                    ? RampControl.FREE_FLOW
                    : controlled(design.type(), draft.movement());
            int structureLevel = stackTiers != null
                    ? stackTiers.structureLevels()[index]
                    : 1 + index % design.structureLevels();
            RampRoute route = stackTiers != null
                    ? stackTierRoute(
                            site,
                            draft,
                            stackTiers.departureLocks()[index],
                            stackTiers.arrivalLocks()[index],
                            stackTiers.departureTargets().get(index),
                            stackTiers.arrivalTargets().get(index),
                            stackTiers.tierWindows().get(index))
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

    private List<InterchangeConnection> applyBuiltInStructureLevels(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            Set<ApproachDirection> approaches,
            List<InterchangeConnection> connections,
            List<InterchangeAuxiliaryLane> auxiliaryLanes) {
        if (design.type() == InterchangeType.STACK) {
            return connections;
        }

        List<ConnectionDraft> paths = new ArrayList<>(
                connections.size() + auxiliaryLanes.size());
        for (InterchangeConnection connection : connections) {
            paths.add(new ConnectionDraft(
                    connection.movement(), connection.route(), connection.form()));
        }
        for (InterchangeAuxiliaryLane auxiliaryLane : auxiliaryLanes) {
            paths.add(new ConnectionDraft(
                    auxiliaryLane.mainlineMovement(),
                    auxiliaryLane.route(),
                    RampForm.MAINLINE));
        }
        paths.addAll(arterialConflictDrafts(site, approaches));

        double[] departureLocks = new double[paths.size()];
        double[] arrivalLocks = new double[paths.size()];
        java.util.Arrays.fill(departureLocks, TERMINAL_GRADE_LOCK_BLOCKS);
        java.util.Arrays.fill(arrivalLocks, TERMINAL_GRADE_LOCK_BLOCKS);
        for (int left = 0; left < paths.size(); left++) {
            for (int right = left + 1; right < paths.size(); right++) {
                ConnectionDraft leftPath = paths.get(left);
                ConnectionDraft rightPath = paths.get(right);
                if (leftPath.movement().from() == rightPath.movement().from()) {
                    SharedTerminalRun shared = sharedDepartureRun(
                            leftPath.route(), rightPath.route());
                    departureLocks[left] = StrictMath.max(
                            departureLocks[left], shared.leftBlocks());
                    departureLocks[right] = StrictMath.max(
                            departureLocks[right], shared.rightBlocks());
                }
                if (leftPath.movement().to() == rightPath.movement().to()) {
                    SharedTerminalRun shared = sharedArrivalRun(
                            leftPath.route(), rightPath.route());
                    arrivalLocks[left] = StrictMath.max(
                            arrivalLocks[left], shared.leftBlocks());
                    arrivalLocks[right] = StrictMath.max(
                            arrivalLocks[right], shared.rightBlocks());
                }
            }
        }

        double[] firstConflict = new double[connections.size()];
        double[] lastConflict = new double[connections.size()];
        boolean[] hasConflict = new boolean[connections.size()];
        String[] firstConflictSource = new String[connections.size()];
        java.util.Arrays.fill(firstConflict, Double.POSITIVE_INFINITY);
        java.util.Arrays.fill(lastConflict, Double.NEGATIVE_INFINITY);
        for (int left = 0; left < paths.size(); left++) {
            for (int right = left + 1; right < paths.size(); right++) {
                if (!isTurningConnection(left, connections)
                        && !isTurningConnection(right, connections)) {
                    continue;
                }
                ConnectionDraft leftPath = paths.get(left);
                ConnectionDraft rightPath = paths.get(right);
                double leftSharedDeparture = departureLocks[left];
                double rightSharedDeparture = departureLocks[right];
                double leftSharedArrival = arrivalLocks[left];
                double rightSharedArrival = arrivalLocks[right];
                if (leftPath.movement().from() == rightPath.movement().from()) {
                    SharedTerminalRun shared = sharedDepartureRun(
                            leftPath.route(), rightPath.route());
                    leftSharedDeparture = StrictMath.max(
                            leftSharedDeparture, shared.leftBlocks());
                    rightSharedDeparture = StrictMath.max(
                            rightSharedDeparture, shared.rightBlocks());
                }
                if (leftPath.movement().to() == rightPath.movement().to()) {
                    SharedTerminalRun shared = sharedArrivalRun(
                            leftPath.route(), rightPath.route());
                    leftSharedArrival = StrictMath.max(
                            leftSharedArrival, shared.leftBlocks());
                    rightSharedArrival = StrictMath.max(
                            rightSharedArrival, shared.rightBlocks());
                }
                ProfileConflictWindow window = profileConflictWindow(
                        leftPath,
                        rightPath,
                        departureLocks[left],
                        departureLocks[right],
                        arrivalLocks[left],
                        arrivalLocks[right],
                        leftSharedDeparture,
                        rightSharedDeparture,
                        leftSharedArrival,
                        rightSharedArrival);
                if (isTurningConnection(left, connections)
                        && window.leftConflict()) {
                    hasConflict[left] = true;
                    if (window.leftStart() < firstConflict[left]) {
                        firstConflictSource[left] = paths.get(right).movement().toString();
                    }
                    firstConflict[left] = StrictMath.min(
                            firstConflict[left], window.leftStart());
                    lastConflict[left] = StrictMath.max(
                            lastConflict[left], window.leftEnd());
                }
                if (isTurningConnection(right, connections)
                        && window.rightConflict()) {
                    hasConflict[right] = true;
                    if (window.rightStart() < firstConflict[right]) {
                        firstConflictSource[right] = paths.get(left).movement().toString();
                    }
                    firstConflict[right] = StrictMath.min(
                            firstConflict[right], window.rightStart());
                    lastConflict[right] = StrictMath.max(
                            lastConflict[right], window.rightEnd());
                }
            }
        }

        List<InterchangeConnection> profiled = new ArrayList<>(connections.size());
        for (int index = 0; index < connections.size(); index++) {
            InterchangeConnection connection = connections.get(index);
            RampRoute route = connection.route();
            int structureLevel = connection.structureLevel();
            if (connection.form() != RampForm.MAINLINE) {
                if (design.type() == InterchangeType.SPUI) {
                    route = signalizedCoreRoute(design, site, connection);
                    structureLevel = 1;
                    profiled.add(new InterchangeConnection(
                            connection.movement(),
                            route,
                            connection.form(),
                            connection.control(),
                            structureLevel));
                    continue;
                }
                IllegalArgumentException rejection = null;
                for (int candidateLevel : preferredStructureLevels(
                        design, site, connection)) {
                    try {
                        route = builtInStructureLevelRoute(
                                design,
                                site,
                                connection.movement(),
                                connection.route(),
                                candidateLevel,
                                departureLocks[index],
                                arrivalLocks[index],
                                hasConflict[index],
                                firstConflictSource[index],
                                firstConflict[index],
                                lastConflict[index]);
                        structureLevel = candidateLevel;
                        rejection = null;
                        break;
                    } catch (IllegalArgumentException candidateRejection) {
                        if (rejection == null) {
                            rejection = candidateRejection;
                        } else {
                            rejection.addSuppressed(candidateRejection);
                        }
                    }
                }
                if (rejection != null) {
                    route = connection.route();
                    structureLevel = connection.structureLevel();
                }
            }
            profiled.add(new InterchangeConnection(
                    connection.movement(),
                    route,
                    connection.form(),
                    connection.control(),
                    structureLevel));
        }
        return List.copyOf(profiled);
    }

    private RampRoute signalizedCoreRoute(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            InterchangeConnection connection) {
        RampRoute route = connection.route();
        RampCenterline centerline = route.centerline();
        HalfBlockElevation start = centerline.startElevation();
        HalfBlockElevation end = centerline.endElevation();
        HalfBlockElevation core = lowerCenterElevation(site);
        int departureRun = gradePlanner.minimumRunBlocks(start, core);
        int arrivalRun = gradePlanner.minimumRunBlocks(core, end);
        double departureStart = TERMINAL_GRADE_LOCK_BLOCKS;
        double departureEnd = departureStart + departureRun;
        double arrivalEnd = centerline.lengthBlocks() - TERMINAL_GRADE_LOCK_BLOCKS;
        double arrivalStart = arrivalEnd - arrivalRun;
        if (departureEnd > arrivalStart + 1.0e-9) {
            throw new IllegalArgumentException(
                    design.id() + " movement " + connection.movement()
                            + " has no room for a level signalized core");
        }
        List<RampElevationKeyframe> profile = new ArrayList<>();
        appendKeyframe(profile, 0.0, start);
        appendKeyframe(profile, departureStart, start);
        appendKeyframe(profile, departureEnd, core);
        appendKeyframe(profile, arrivalStart, core);
        appendKeyframe(profile, arrivalEnd, end);
        appendKeyframe(profile, centerline.lengthBlocks(), end);
        return new RampRoute(
                centerline.withElevationProfile(profile),
                route.widthBlocks());
    }

    private List<ConnectionDraft> arterialConflictDrafts(
            InterchangeGeometrySite site,
            Set<ApproachDirection> approaches) {
        List<ConnectionDraft> result = new ArrayList<>(approaches.size() * 2);
        for (ApproachDirection direction : approaches) {
            double run = site.approachRunBlocks();
            RoadHeading heading = switch (direction) {
                case NORTH -> RoadHeading.SOUTH;
                case EAST -> RoadHeading.WEST;
                case SOUTH -> RoadHeading.NORTH;
                case WEST -> RoadHeading.EAST;
            };
            HalfBlockElevation nativeElevation = switch (direction) {
                case EAST, WEST -> site.xRoadNativeElevation();
                case NORTH, SOUTH -> site.zRoadNativeElevation();
            };
            for (double crossOffset : List.of(-8.0, 8.0)) {
                PlanarPoint startPoint = switch (direction) {
                    case NORTH -> new PlanarPoint(
                            site.center().x() + crossOffset,
                            site.center().z() - run);
                    case EAST -> new PlanarPoint(
                            site.center().x() + run,
                            site.center().z() + crossOffset);
                    case SOUTH -> new PlanarPoint(
                            site.center().x() + crossOffset,
                            site.center().z() + run);
                    case WEST -> new PlanarPoint(
                            site.center().x() - run,
                            site.center().z() + crossOffset);
                };
                RampCenterline centerline = new RampPathBuilder(
                        standard, startPoint, heading)
                        .straight(run * 2.0)
                        .build(List.of(
                                new RampElevationKeyframe(0.0, nativeElevation),
                                new RampElevationKeyframe(
                                        run, site.centerElevation(direction)),
                                new RampElevationKeyframe(run * 2.0, nativeElevation)));
                result.add(new ConnectionDraft(
                        new InterchangeMovement(
                                direction,
                                direction.opposite(),
                                MovementKind.STRAIGHT),
                        new RampRoute(centerline, 16),
                        RampForm.MAINLINE));
            }
        }
        return List.copyOf(result);
    }

    private List<Integer> preferredStructureLevels(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            InterchangeConnection connection) {
        List<Integer> levels = new ArrayList<>(design.structureLevels());
        for (int level = 1; level <= design.structureLevels(); level++) {
            levels.add(level);
        }
        HalfBlockElevation start = connection.route().centerline().startElevation();
        HalfBlockElevation end = connection.route().centerline().endElevation();
        levels.sort(Comparator
                .comparingInt((Integer level) -> {
                    HalfBlockElevation tier = structureLevelElevation(
                            site, design.structureLevels(), level);
                    return gradePlanner.minimumRunBlocks(start, tier)
                            + gradePlanner.minimumRunBlocks(tier, end);
                })
                .thenComparingInt(level -> StrictMath.abs(
                        level - connection.structureLevel()))
                .thenComparingInt(Integer::intValue));
        return List.copyOf(levels);
    }

    private static boolean isTurningConnection(
            int index,
            List<InterchangeConnection> connections) {
        return index < connections.size()
                && connections.get(index).form() != RampForm.MAINLINE;
    }

    private RampRoute builtInStructureLevelRoute(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            InterchangeMovement movement,
            RampRoute route,
            int structureLevel,
            double departureLock,
            double arrivalLock,
            boolean hasConflict,
            String firstConflictSource,
            double firstConflict,
            double lastConflict) {
        RampCenterline centerline = route.centerline();
        HalfBlockElevation start = centerline.startElevation();
        HalfBlockElevation end = centerline.endElevation();
        HalfBlockElevation tier = structureLevelElevation(
                site, design.structureLevels(), structureLevel);
        int departureGradeRun = gradePlanner.minimumRunBlocks(start, tier);
        int arrivalGradeRun = gradePlanner.minimumRunBlocks(tier, end);
        double departureGradeStart = hasConflict && departureGradeRun != 0
                ? firstConflict - departureGradeRun : departureLock;
        double departureGradeEnd = departureGradeStart + departureGradeRun;
        double arrivalGradeEnd = hasConflict && arrivalGradeRun != 0
                ? lastConflict + arrivalGradeRun
                : centerline.lengthBlocks() - arrivalLock;
        double arrivalGradeStart = arrivalGradeEnd - arrivalGradeRun;
        boolean departureFits = departureGradeStart >= departureLock - 1.0e-9;
        boolean arrivalFits = arrivalGradeEnd <= centerline.lengthBlocks()
                - arrivalLock + 1.0e-9;
        boolean conflictCovered = !hasConflict
                || ((departureGradeRun == 0
                || departureGradeEnd <= firstConflict + 1.0e-9)
                && (arrivalGradeRun == 0
                || arrivalGradeStart >= lastConflict - 1.0e-9));
        if (!departureFits
                || !arrivalFits
                || !conflictCovered
                || departureGradeEnd > arrivalGradeStart + 1.0e-9) {
            throw new IllegalArgumentException(
                    design.id() + " movement " + movement
                            + " cannot safely reach built-in structure level "
                            + structureLevel + " outside surface-conflict window "
                            + String.format(
                                    java.util.Locale.ROOT,
                                    "%.3f..%.3f (terminal locks %.3f/%.3f)",
                                    hasConflict ? firstConflict : departureGradeEnd,
                                    hasConflict ? lastConflict : arrivalGradeStart,
                                    departureLock,
                                    arrivalLock)
                            + (firstConflictSource == null
                                    ? "" : " first overlaps " + firstConflictSource));
        }

        List<RampElevationKeyframe> profile = new ArrayList<>();
        appendKeyframe(profile, 0.0, start);
        appendKeyframe(profile, departureGradeStart, start);
        appendKeyframe(profile, departureGradeEnd, tier);
        appendKeyframe(profile, arrivalGradeStart, tier);
        appendKeyframe(profile, arrivalGradeEnd, end);
        appendKeyframe(profile, centerline.lengthBlocks(), end);
        return new RampRoute(
                centerline.withElevationProfile(profile),
                route.widthBlocks());
    }

    private void requirePhysicalStackLevels(
            InterchangeDesign design,
            InterchangeGeometrySite site) {
        if (design.type() != InterchangeType.STACK) {
            return;
        }
        int availableHalfBlocks = StrictMath.abs(
                site.xRoadCenterElevation().halfBlocks()
                        - site.zRoadCenterElevation().halfBlocks());
        int requiredHalfBlocks = Math.multiplyExact(
                design.structureLevels() - 1,
                Math.multiplyExact(standard.minimumVehicleClearanceBlocks(), 2));
        if (availableHalfBlocks < requiredHalfBlocks) {
            throw new IllegalArgumentException(
                    design.id() + " requires four physical levels between its mainline decks"
                            + " (needs " + requiredHalfBlocks / 2.0
                            + " blocks, has " + availableHalfBlocks / 2.0 + ')');
        }
    }

    private StackTierPlan stackTierPlan(
            InterchangeGeometrySite site,
            List<ConnectionDraft> drafts) {
        int size = drafts.size();
        int[] levels = new int[size];
        double[] departureLocks = new double[size];
        double[] arrivalLocks = new double[size];
        java.util.Arrays.fill(departureLocks, 32.0);
        java.util.Arrays.fill(arrivalLocks, 32.0);

        List<Integer> turns = new ArrayList<>();
        HalfBlockElevation lower = lowerCenterElevation(site);
        for (int index = 0; index < size; index++) {
            ConnectionDraft draft = drafts.get(index);
            if (draft.form() == RampForm.MAINLINE) {
                levels[index] = draft.route().centerline().startElevation().compareTo(lower) <= 0
                        ? 1 : 4;
            } else {
                turns.add(index);
            }
        }

        for (int leftPosition = 0; leftPosition < turns.size(); leftPosition++) {
            int leftIndex = turns.get(leftPosition);
            ConnectionDraft left = drafts.get(leftIndex);
            for (int rightPosition = leftPosition + 1;
                    rightPosition < turns.size(); rightPosition++) {
                int rightIndex = turns.get(rightPosition);
                ConnectionDraft right = drafts.get(rightIndex);
                if (left.movement().from() == right.movement().from()) {
                    SharedTerminalRun shared = sharedDepartureRun(
                            left.route(), right.route());
                    departureLocks[leftIndex] = StrictMath.max(
                            departureLocks[leftIndex], shared.leftBlocks());
                    departureLocks[rightIndex] = StrictMath.max(
                            departureLocks[rightIndex], shared.rightBlocks());
                }
                if (left.movement().to() == right.movement().to()) {
                    SharedTerminalRun shared = sharedArrivalRun(
                            left.route(), right.route());
                    arrivalLocks[leftIndex] = StrictMath.max(
                            arrivalLocks[leftIndex], shared.leftBlocks());
                    arrivalLocks[rightIndex] = StrictMath.max(
                            arrivalLocks[rightIndex], shared.rightBlocks());
                }
            }
        }

        List<StackCrossing> crossings = new ArrayList<>();
        for (int leftPosition = 0; leftPosition < turns.size(); leftPosition++) {
            int leftIndex = turns.get(leftPosition);
            for (int rightPosition = leftPosition + 1;
                    rightPosition < turns.size(); rightPosition++) {
                int rightIndex = turns.get(rightPosition);
                SurfaceConflictWindow window = interiorSurfaceConflictWindow(
                        drafts.get(leftIndex),
                        drafts.get(rightIndex),
                        departureLocks[leftIndex],
                        departureLocks[rightIndex],
                        arrivalLocks[leftIndex],
                        arrivalLocks[rightIndex]);
                if (window.conflict()) {
                    crossings.add(new StackCrossing(
                            leftIndex,
                            rightIndex,
                            window.leftStart(),
                            window.leftEnd(),
                            window.rightStart(),
                            window.rightEnd()));
                }
            }
        }

        List<List<StackTierWindow>> tierWindows = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            tierWindows.add(new ArrayList<>());
        }
        if (!assignStackCrossings(
                site,
                drafts,
                crossings,
                0,
                departureLocks,
                arrivalLocks,
                tierWindows)) {
            throw new IllegalArgumentException(
                    "Stack turning paths cannot be scheduled monotonically across four"
                            + " physical levels: " + crossings);
        }

        for (int index : turns) {
            List<StackTierWindow> windows = mergeTierWindows(tierWindows.get(index));
            tierWindows.set(index, windows);
            levels[index] = representativeLevel(site, drafts.get(index), windows);
        }
        StackTerminalTargets terminalTargets = stackTerminalTargets(
                site, drafts, tierWindows);
        return new StackTierPlan(
                levels,
                departureLocks,
                arrivalLocks,
                tierWindows.stream().map(List::copyOf).toList(),
                terminalTargets.departures(),
                terminalTargets.arrivals());
    }

    private boolean assignStackCrossings(
            InterchangeGeometrySite site,
            List<ConnectionDraft> drafts,
            List<StackCrossing> crossings,
            int position,
            double[] departureLocks,
            double[] arrivalLocks,
            List<List<StackTierWindow>> tierWindows) {
        if (position == crossings.size()) {
            return usesBothIntermediateStackLevels(tierWindows)
                    && completeStackScheduleFeasible(
                            site,
                            drafts,
                            departureLocks,
                            arrivalLocks,
                            tierWindows);
        }
        StackCrossing crossing = crossings.get(position);
        for (TierPair pair : candidateTierPairs(site, drafts, crossing)) {
            StackTierWindow leftWindow = new StackTierWindow(
                    crossing.leftStart(), crossing.leftEnd(), pair.leftLevel());
            StackTierWindow rightWindow = new StackTierWindow(
                    crossing.rightStart(), crossing.rightEnd(), pair.rightLevel());
            List<StackTierWindow> left = tierWindows.get(crossing.leftRoute());
            List<StackTierWindow> right = tierWindows.get(crossing.rightRoute());
            left.add(leftWindow);
            right.add(rightWindow);
            boolean feasible = stackWindowsFeasible(
                    site,
                    drafts.get(crossing.leftRoute()),
                    left)
                    && stackWindowsFeasible(
                            site,
                            drafts.get(crossing.rightRoute()),
                            right);
            if (feasible && assignStackCrossings(
                    site,
                    drafts,
                    crossings,
                    position + 1,
                    departureLocks,
                    arrivalLocks,
                    tierWindows)) {
                return true;
            }
            left.removeLast();
            right.removeLast();
        }
        return false;
    }

    private List<TierPair> candidateTierPairs(
            InterchangeGeometrySite site,
            List<ConnectionDraft> drafts,
            StackCrossing crossing) {
        ConnectionDraft left = drafts.get(crossing.leftRoute());
        ConnectionDraft right = drafts.get(crossing.rightRoute());
        double leftTarget = targetStackLevel(
                site, left, (crossing.leftStart() + crossing.leftEnd()) / 2.0);
        double rightTarget = targetStackLevel(
                site, right, (crossing.rightStart() + crossing.rightEnd()) / 2.0);
        List<TierPair> result = new ArrayList<>();
        for (int leftLevel = 1; leftLevel <= 4; leftLevel++) {
            for (int rightLevel = 1; rightLevel <= 4; rightLevel++) {
                if (leftLevel != rightLevel) {
                    result.add(new TierPair(leftLevel, rightLevel));
                }
            }
        }
        result.sort(Comparator
                .comparingDouble((TierPair pair) ->
                        StrictMath.abs(pair.leftLevel() - leftTarget)
                                + StrictMath.abs(pair.rightLevel() - rightTarget))
                .thenComparingInt(pair -> StrictMath.abs(
                        pair.leftLevel() - pair.rightLevel()))
                .thenComparingInt(TierPair::leftLevel)
                .thenComparingInt(TierPair::rightLevel));
        return result;
    }

    private double targetStackLevel(
            InterchangeGeometrySite site,
            ConnectionDraft draft,
            double station) {
        HalfBlockElevation lower = lowerCenterElevation(site);
        RampCenterline centerline = draft.route().centerline();
        double progress = station / centerline.lengthBlocks();
        return centerline.startElevation().compareTo(lower) <= 0
                ? 1.0 + 3.0 * progress
                : 4.0 - 3.0 * progress;
    }

    private boolean stackWindowsFeasible(
            InterchangeGeometrySite site,
            ConnectionDraft draft,
            List<StackTierWindow> unsortedWindows) {
        RampCenterline centerline = draft.route().centerline();
        HalfBlockElevation start = centerline.startElevation();
        HalfBlockElevation end = centerline.endElevation();
        boolean ascending = start.compareTo(end) <= 0;
        List<StackTierWindow> windows = unsortedWindows.stream()
                .sorted(Comparator.comparingDouble(StackTierWindow::startBlocks))
                .toList();
        double previousEnd = 0.0;
        HalfBlockElevation previousElevation = start;
        for (StackTierWindow window : windows) {
            HalfBlockElevation elevation = stackTierElevation(site, window.level());
            if ((ascending && elevation.compareTo(previousElevation) < 0)
                    || (!ascending && elevation.compareTo(previousElevation) > 0)) {
                return false;
            }
            if (window.startBlocks() < previousEnd - 1.0e-9) {
                if (!elevation.equals(previousElevation)) {
                    return false;
                }
                previousEnd = StrictMath.max(previousEnd, window.endBlocks());
                continue;
            }
            if (gradePlanner.minimumRunBlocks(previousElevation, elevation)
                    > window.startBlocks() - previousEnd + 1.0e-9) {
                return false;
            }
            previousEnd = window.endBlocks();
            previousElevation = elevation;
        }
        if ((ascending && end.compareTo(previousElevation) < 0)
                || (!ascending && end.compareTo(previousElevation) > 0)) {
            return false;
        }
        return gradePlanner.minimumRunBlocks(previousElevation, end)
                <= centerline.lengthBlocks() - previousEnd + 1.0e-9;
    }

    private RampRoute stackTierRoute(
            InterchangeGeometrySite site,
            ConnectionDraft draft,
            double departureLock,
            double arrivalLock,
            StackTerminalTarget departureTarget,
            StackTerminalTarget arrivalTarget,
            List<StackTierWindow> tierWindows) {
        RampRoute route = draft.route();
        if (draft.form() == RampForm.MAINLINE) {
            return route;
        }
        RampCenterline centerline = route.centerline();
        HalfBlockElevation start = centerline.startElevation();
        HalfBlockElevation end = centerline.endElevation();
        List<RampElevationKeyframe> profile = new ArrayList<>();
        appendKeyframe(profile, 0.0, start);
        appendKeyframe(
                profile,
                departureTarget.stationBlocks(),
                stackTierElevation(site, departureTarget.level()));
        appendKeyframe(
                profile,
                departureLock,
                stackTierElevation(site, departureTarget.level()));
        for (StackTierWindow window : tierWindows) {
            HalfBlockElevation elevation = stackTierElevation(site, window.level());
            appendKeyframe(profile, window.startBlocks(), elevation);
            appendKeyframe(profile, window.endBlocks(), elevation);
        }
        appendKeyframe(
                profile,
                centerline.lengthBlocks() - arrivalLock,
                stackTierElevation(site, arrivalTarget.level()));
        appendKeyframe(
                profile,
                arrivalTarget.stationBlocks(),
                stackTierElevation(site, arrivalTarget.level()));
        appendKeyframe(profile, centerline.lengthBlocks(), end);
        return new RampRoute(centerline.withElevationProfile(profile), route.widthBlocks());
    }

    private boolean completeStackScheduleFeasible(
            InterchangeGeometrySite site,
            List<ConnectionDraft> drafts,
            double[] departureLocks,
            double[] arrivalLocks,
            List<List<StackTierWindow>> tierWindows) {
        StackTerminalTargets targets = stackTerminalTargets(site, drafts, tierWindows);
        for (int index = 0; index < drafts.size(); index++) {
            ConnectionDraft draft = drafts.get(index);
            if (draft.form() == RampForm.MAINLINE) {
                continue;
            }
            RampCenterline centerline = draft.route().centerline();
            List<StackProfilePoint> points = new ArrayList<>();
            points.add(new StackProfilePoint(0.0, centerline.startElevation()));
            StackTerminalTarget departure = targets.departures().get(index);
            points.add(new StackProfilePoint(
                    departure.stationBlocks(),
                    stackTierElevation(site, departure.level())));
            points.add(new StackProfilePoint(
                    departureLocks[index],
                    stackTierElevation(site, departure.level())));
            for (StackTierWindow window : mergeTierWindows(tierWindows.get(index))) {
                HalfBlockElevation elevation = stackTierElevation(site, window.level());
                points.add(new StackProfilePoint(window.startBlocks(), elevation));
                points.add(new StackProfilePoint(window.endBlocks(), elevation));
            }
            StackTerminalTarget arrival = targets.arrivals().get(index);
            points.add(new StackProfilePoint(
                    centerline.lengthBlocks() - arrivalLocks[index],
                    stackTierElevation(site, arrival.level())));
            points.add(new StackProfilePoint(
                    arrival.stationBlocks(),
                    stackTierElevation(site, arrival.level())));
            points.add(new StackProfilePoint(
                    centerline.lengthBlocks(), centerline.endElevation()));
            points.sort(Comparator.comparingDouble(StackProfilePoint::stationBlocks));
            if (!stackProfileFeasible(points, centerline.startElevation(), centerline.endElevation())) {
                return false;
            }
        }
        return true;
    }

    private boolean stackProfileFeasible(
            List<StackProfilePoint> points,
            HalfBlockElevation start,
            HalfBlockElevation end) {
        boolean ascending = start.compareTo(end) <= 0;
        StackProfilePoint previous = points.getFirst();
        for (int index = 1; index < points.size(); index++) {
            StackProfilePoint current = points.get(index);
            double run = current.stationBlocks() - previous.stationBlocks();
            if (run < -1.0e-9) {
                return false;
            }
            if ((ascending && current.elevation().compareTo(previous.elevation()) < 0)
                    || (!ascending
                            && current.elevation().compareTo(previous.elevation()) > 0)) {
                return false;
            }
            int requiredRun = gradePlanner.minimumRunBlocks(
                    previous.elevation(), current.elevation());
            if (requiredRun > run + 1.0e-9) {
                return false;
            }
            previous = current;
        }
        return true;
    }

    private StackTerminalTargets stackTerminalTargets(
            InterchangeGeometrySite site,
            List<ConnectionDraft> drafts,
            List<List<StackTierWindow>> tierWindows) {
        List<StackTerminalTarget> departures = new ArrayList<>(
                java.util.Collections.nCopies(drafts.size(), null));
        List<StackTerminalTarget> arrivals = new ArrayList<>(
                java.util.Collections.nCopies(drafts.size(), null));

        for (ApproachDirection direction : CLOCKWISE_APPROACHES) {
            int departureController = -1;
            StackTierWindow firstWindow = null;
            int arrivalController = -1;
            StackTierWindow lastWindow = null;
            for (int index = 0; index < drafts.size(); index++) {
                ConnectionDraft draft = drafts.get(index);
                if (draft.form() == RampForm.MAINLINE || tierWindows.get(index).isEmpty()) {
                    continue;
                }
                List<StackTierWindow> windows = tierWindows.get(index).stream()
                        .sorted(Comparator.comparingDouble(StackTierWindow::startBlocks))
                        .toList();
                if (draft.movement().from() == direction
                        && (firstWindow == null
                                || windows.getFirst().startBlocks()
                                        < firstWindow.startBlocks())) {
                    departureController = index;
                    firstWindow = windows.getFirst();
                }
                if (draft.movement().to() == direction
                        && (lastWindow == null
                                || windows.getLast().endBlocks() > lastWindow.endBlocks())) {
                    arrivalController = index;
                    lastWindow = windows.getLast();
                }
            }
            if (departureController >= 0) {
                HalfBlockElevation sourceElevation = drafts.get(departureController)
                        .route().centerline().startElevation();
                StackTerminalTarget target = new StackTerminalTarget(
                        STACK_SHARED_TRUNK_BLOCKS,
                        departureStackLevel(site, sourceElevation));
                for (int index = 0; index < drafts.size(); index++) {
                    ConnectionDraft draft = drafts.get(index);
                    if (draft.form() != RampForm.MAINLINE
                            && draft.movement().from() == direction) {
                        departures.set(index, target);
                    }
                }
            }
            if (arrivalController >= 0) {
                HalfBlockElevation destinationElevation = drafts.get(arrivalController)
                        .route().centerline().endElevation();
                for (int index = 0; index < drafts.size(); index++) {
                    ConnectionDraft draft = drafts.get(index);
                    if (draft.form() != RampForm.MAINLINE
                            && draft.movement().to() == direction) {
                        arrivals.set(index, new StackTerminalTarget(
                                draft.route().centerline().lengthBlocks()
                                        - STACK_SHARED_TRUNK_BLOCKS,
                                arrivalStackLevel(site, destinationElevation)));
                    }
                }
            }
        }

        for (int index = 0; index < drafts.size(); index++) {
            ConnectionDraft draft = drafts.get(index);
            RampCenterline centerline = draft.route().centerline();
            if (departures.get(index) == null) {
                departures.set(index, new StackTerminalTarget(
                        0.0, endpointStackLevel(site, centerline.startElevation())));
            }
            if (arrivals.get(index) == null) {
                arrivals.set(index, new StackTerminalTarget(
                        centerline.lengthBlocks(),
                        endpointStackLevel(site, centerline.endElevation())));
            }
        }
        return new StackTerminalTargets(List.copyOf(departures), List.copyOf(arrivals));
    }

    private static int endpointStackLevel(
            InterchangeGeometrySite site,
            HalfBlockElevation elevation) {
        return elevation.equals(lowerCenterElevation(site)) ? 1 : 4;
    }

    private static int departureStackLevel(
            InterchangeGeometrySite site,
            HalfBlockElevation mainlineElevation) {
        return mainlineElevation.equals(lowerCenterElevation(site)) ? 3 : 2;
    }

    private static int arrivalStackLevel(
            InterchangeGeometrySite site,
            HalfBlockElevation mainlineElevation) {
        return mainlineElevation.equals(lowerCenterElevation(site)) ? 2 : 3;
    }

    private static List<StackTierWindow> mergeTierWindows(
            List<StackTierWindow> windows) {
        List<StackTierWindow> sorted = windows.stream()
                .sorted(Comparator.comparingDouble(StackTierWindow::startBlocks))
                .toList();
        List<StackTierWindow> merged = new ArrayList<>();
        for (StackTierWindow window : sorted) {
            if (!merged.isEmpty()) {
                StackTierWindow previous = merged.getLast();
                if (window.startBlocks() <= previous.endBlocks() + 1.0e-9) {
                    if (window.level() != previous.level()) {
                        throw new IllegalArgumentException(
                                "Overlapping stack crossing windows use different levels");
                    }
                    merged.set(merged.size() - 1, new StackTierWindow(
                            previous.startBlocks(),
                            StrictMath.max(previous.endBlocks(), window.endBlocks()),
                            previous.level()));
                    continue;
                }
            }
            merged.add(window);
        }
        return List.copyOf(merged);
    }

    private static boolean usesBothIntermediateStackLevels(
            List<List<StackTierWindow>> tierWindows) {
        boolean levelTwo = false;
        boolean levelThree = false;
        for (List<StackTierWindow> windows : tierWindows) {
            for (StackTierWindow window : windows) {
                levelTwo |= window.level() == 2;
                levelThree |= window.level() == 3;
            }
        }
        return levelTwo && levelThree;
    }

    private int representativeLevel(
            InterchangeGeometrySite site,
            ConnectionDraft draft,
            List<StackTierWindow> windows) {
        if (!windows.isEmpty()) {
            double center = draft.route().centerline().lengthBlocks() / 2.0;
            return windows.stream()
                    .min(Comparator.comparingDouble(window -> StrictMath.abs(
                            (window.startBlocks() + window.endBlocks()) / 2.0 - center)))
                    .orElseThrow()
                    .level();
        }
        double target = targetStackLevel(
                site, draft, draft.route().centerline().lengthBlocks() / 2.0);
        return (int) StrictMath.max(1.0, StrictMath.min(4.0, StrictMath.rint(target)));
    }

    private HalfBlockElevation stackTierElevation(
            InterchangeGeometrySite site,
            int structureLevel) {
        HalfBlockElevation lower = lowerCenterElevation(site);
        HalfBlockElevation upper = upperCenterElevation(site);
        int clearanceHalfBlocks = Math.multiplyExact(
                standard.minimumVehicleClearanceBlocks(), 2);
        return switch (structureLevel) {
            case 1 -> lower;
            case 2 -> lower.plusHalfBlocks(clearanceHalfBlocks);
            case 3 -> upper.plusHalfBlocks(-clearanceHalfBlocks);
            case 4 -> upper;
            default -> throw new IllegalArgumentException(
                    "A four-level stack cannot use tier " + structureLevel);
        };
    }

    private static SharedTerminalRun sharedDepartureRun(
            RampRoute left,
            RampRoute right) {
        double overlap = (left.widthBlocks() + right.widthBlocks()) / 2.0;
        double leftBlocks = 0.0;
        double rightBlocks = 0.0;
        RouteSpatialIndex rightRoute = new RouteSpatialIndex(right);
        for (var sample : left.centerline().samples()) {
            RouteProximity nearest = rightRoute.nearestWithin(sample.point(), overlap);
            if (nearest == null) {
                break;
            }
            leftBlocks = sample.stationBlocks();
            rightBlocks = nearest.stationBlocks();
        }
        return new SharedTerminalRun(leftBlocks, rightBlocks);
    }

    private static SharedTerminalRun sharedArrivalRun(
            RampRoute left,
            RampRoute right) {
        double overlap = (left.widthBlocks() + right.widthBlocks()) / 2.0;
        double leftBlocks = 0.0;
        double rightBlocks = 0.0;
        List<net.austizz.lostcitiesroadfixes.interchange.geometry.RampCenterlineSample>
                samples = left.centerline().samples();
        RouteSpatialIndex rightRoute = new RouteSpatialIndex(right);
        for (int index = samples.size() - 1; index >= 0; index--) {
            var sample = samples.get(index);
            RouteProximity nearest = rightRoute.nearestWithin(sample.point(), overlap);
            if (nearest == null) {
                break;
            }
            leftBlocks = left.centerline().lengthBlocks() - sample.stationBlocks();
            rightBlocks = right.centerline().lengthBlocks() - nearest.stationBlocks();
        }
        return new SharedTerminalRun(leftBlocks, rightBlocks);
    }

    private static SurfaceConflictWindow interiorSurfaceConflictWindow(
            ConnectionDraft left,
            ConnectionDraft right,
            double leftDepartureLock,
            double rightDepartureLock,
            double leftArrivalLock,
            double rightArrivalLock) {
        double overlap = (left.route().widthBlocks() + right.route().widthBlocks()) / 2.0
                - 0.5;
        double leftStart = Double.POSITIVE_INFINITY;
        double leftEnd = Double.NEGATIVE_INFINITY;
        double rightStart = Double.POSITIVE_INFINITY;
        double rightEnd = Double.NEGATIVE_INFINITY;
        List<net.austizz.lostcitiesroadfixes.interchange.geometry.RampCenterlineSample>
                samples = left.route().centerline().samples();
        RouteSpatialIndex rightRoute = new RouteSpatialIndex(right.route());
        for (int index = 0; index < samples.size(); index++) {
            var sample = samples.get(index);
            RouteProximity nearest = rightRoute.nearestWithin(sample.point(), overlap);
            if (nearest == null) {
                continue;
            }
            boolean sharedDeparture = left.movement().from() == right.movement().from()
                    && sample.stationBlocks() <= leftDepartureLock + 1.0e-9
                    && nearest.stationBlocks() <= rightDepartureLock + 1.0e-9;
            boolean sharedArrival = left.movement().to() == right.movement().to()
                    && left.route().centerline().lengthBlocks()
                            - sample.stationBlocks() <= leftArrivalLock + 1.0e-9
                    && right.route().centerline().lengthBlocks()
                            - nearest.stationBlocks() <= rightArrivalLock + 1.0e-9;
            if (!sharedDeparture && !sharedArrival) {
                leftStart = StrictMath.min(leftStart, sample.stationBlocks());
                leftEnd = StrictMath.max(leftEnd, sample.stationBlocks());
                rightStart = StrictMath.min(rightStart, nearest.stationBlocks());
                rightEnd = StrictMath.max(rightEnd, nearest.stationBlocks());
            }
        }
        return new SurfaceConflictWindow(
                Double.isFinite(leftStart),
                leftStart,
                leftEnd,
                rightStart,
                rightEnd);
    }

    private static ProfileConflictWindow profileConflictWindow(
            ConnectionDraft left,
            ConnectionDraft right,
            double leftDepartureLock,
            double rightDepartureLock,
            double leftArrivalLock,
            double rightArrivalLock,
            double leftSharedDeparture,
            double rightSharedDeparture,
            double leftSharedArrival,
            double rightSharedArrival) {
        // Rasterization tests block centres rather than mathematical strip
        // intersections. Reserve a diagonal-cell margin so a grade is fully
        // complete before two paved footprints can share a block column.
        double overlap = (left.route().widthBlocks() + right.route().widthBlocks()) / 2.0
                + RASTERIZED_SURFACE_MARGIN_BLOCKS;
        double leftStart = Double.POSITIVE_INFINITY;
        double leftEnd = Double.NEGATIVE_INFINITY;
        double rightStart = Double.POSITIVE_INFINITY;
        double rightEnd = Double.NEGATIVE_INFINITY;
        double leftMutableEnd = left.route().centerline().lengthBlocks()
                - leftArrivalLock;
        double rightMutableEnd = right.route().centerline().lengthBlocks()
                - rightArrivalLock;
        List<net.austizz.lostcitiesroadfixes.interchange.geometry.RampCenterlineSample>
                samples = left.route().centerline().samples();
        RouteSpatialIndex rightRoute = new RouteSpatialIndex(right.route());
        for (var sample : samples) {
            RouteProximity nearest = rightRoute.nearestWithin(sample.point(), overlap);
            if (nearest == null) {
                continue;
            }
            boolean sharedDeparture = left.movement().from() == right.movement().from()
                    && sample.stationBlocks() <= leftSharedDeparture + 1.0e-9
                    && nearest.stationBlocks() <= rightSharedDeparture + 1.0e-9;
            boolean sharedArrival = left.movement().to() == right.movement().to()
                    && left.route().centerline().lengthBlocks()
                            - sample.stationBlocks() <= leftSharedArrival + 1.0e-9
                    && right.route().centerline().lengthBlocks()
                            - nearest.stationBlocks() <= rightSharedArrival + 1.0e-9;
            if (sharedDeparture || sharedArrival) {
                continue;
            }
            if (sample.stationBlocks() > leftDepartureLock + 1.0e-9
                    && sample.stationBlocks() < leftMutableEnd - 1.0e-9) {
                leftStart = StrictMath.min(leftStart, sample.stationBlocks());
                leftEnd = StrictMath.max(leftEnd, sample.stationBlocks());
            }
            if (nearest.stationBlocks() > rightDepartureLock + 1.0e-9
                    && nearest.stationBlocks() < rightMutableEnd - 1.0e-9) {
                rightStart = StrictMath.min(rightStart, nearest.stationBlocks());
                rightEnd = StrictMath.max(rightEnd, nearest.stationBlocks());
            }
        }
        return new ProfileConflictWindow(
                Double.isFinite(leftStart),
                leftStart,
                leftEnd,
                Double.isFinite(rightStart),
                rightStart,
                rightEnd);
    }

    private static void appendKeyframe(
            List<RampElevationKeyframe> profile,
            double station,
            HalfBlockElevation elevation) {
        if (!profile.isEmpty()) {
            RampElevationKeyframe previous = profile.getLast();
            if (station < previous.stationBlocks() - 1.0e-9) {
                throw new IllegalArgumentException("Elevation keyframes moved backwards");
            }
            if (StrictMath.abs(station - previous.stationBlocks()) <= 1.0e-9) {
                if (!previous.elevation().equals(elevation)) {
                    throw new IllegalArgumentException(
                            "Elevation changes without horizontal grade run");
                }
                return;
            }
        }
        profile.add(new RampElevationKeyframe(station, elevation));
    }

    private static HalfBlockElevation lowerCenterElevation(InterchangeGeometrySite site) {
        return site.xRoadCenterElevation().compareTo(site.zRoadCenterElevation()) <= 0
                ? site.xRoadCenterElevation() : site.zRoadCenterElevation();
    }

    private static HalfBlockElevation upperCenterElevation(InterchangeGeometrySite site) {
        return site.xRoadCenterElevation().compareTo(site.zRoadCenterElevation()) >= 0
                ? site.xRoadCenterElevation() : site.zRoadCenterElevation();
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

    private record BlueprintRouteDraft(
            InterchangeMovement movement,
            InterchangeMovementBlueprint blueprint,
            RampRoute route) {
    }

    private record StackTierPlan(
            int[] structureLevels,
            double[] departureLocks,
            double[] arrivalLocks,
            List<List<StackTierWindow>> tierWindows,
            List<StackTerminalTarget> departureTargets,
            List<StackTerminalTarget> arrivalTargets) {
    }

    private record StackCrossing(
            int leftRoute,
            int rightRoute,
            double leftStart,
            double leftEnd,
            double rightStart,
            double rightEnd) {
    }

    private record StackTierWindow(double startBlocks, double endBlocks, int level) {
    }

    private record StackTerminalTarget(double stationBlocks, int level) {
    }

    private record StackTerminalTargets(
            List<StackTerminalTarget> departures,
            List<StackTerminalTarget> arrivals) {
    }

    private record StackProfilePoint(
            double stationBlocks,
            HalfBlockElevation elevation) {
    }

    private record TierPair(int leftLevel, int rightLevel) {
    }

    private record SurfaceConflictWindow(
            boolean conflict,
            double leftStart,
            double leftEnd,
            double rightStart,
            double rightEnd) {
    }

    private record ProfileConflictWindow(
            boolean leftConflict,
            double leftStart,
            double leftEnd,
            boolean rightConflict,
            double rightStart,
            double rightEnd) {
    }

    private record SharedTerminalRun(double leftBlocks, double rightBlocks) {
    }

    private record RouteProximity(double stationBlocks, double distanceBlocks) {
    }

    private static final class RouteSpatialIndex {
        private static final double CELL_BLOCKS = 16.0;

        private final List<RampCenterlineSample> samples;
        private final Map<Long, List<Integer>> segmentsByCell = new HashMap<>();

        private RouteSpatialIndex(RampRoute route) {
            samples = route.centerline().samples();
            for (int index = 0; index < samples.size() - 1; index++) {
                PlanarPoint start = samples.get(index).point();
                PlanarPoint end = samples.get(index + 1).point();
                int minimumCellX = cell(StrictMath.min(start.x(), end.x()));
                int maximumCellX = cell(StrictMath.max(start.x(), end.x()));
                int minimumCellZ = cell(StrictMath.min(start.z(), end.z()));
                int maximumCellZ = cell(StrictMath.max(start.z(), end.z()));
                for (int cellZ = minimumCellZ; cellZ <= maximumCellZ; cellZ++) {
                    for (int cellX = minimumCellX; cellX <= maximumCellX; cellX++) {
                        segmentsByCell.computeIfAbsent(
                                key(cellX, cellZ), ignored -> new ArrayList<>())
                                .add(index);
                    }
                }
            }
        }

        private RouteProximity nearestWithin(
                PlanarPoint point,
                double maximumDistanceBlocks) {
            int minimumCellX = cell(point.x() - maximumDistanceBlocks);
            int maximumCellX = cell(point.x() + maximumDistanceBlocks);
            int minimumCellZ = cell(point.z() - maximumDistanceBlocks);
            int maximumCellZ = cell(point.z() + maximumDistanceBlocks);
            RouteProximity nearest = null;
            for (int cellZ = minimumCellZ; cellZ <= maximumCellZ; cellZ++) {
                for (int cellX = minimumCellX; cellX <= maximumCellX; cellX++) {
                    List<Integer> segments = segmentsByCell.get(key(cellX, cellZ));
                    if (segments == null) {
                        continue;
                    }
                    for (int segment : segments) {
                        RouteProximity candidate = proximity(segment, point);
                        if (candidate.distanceBlocks() >= maximumDistanceBlocks) {
                            continue;
                        }
                        if (nearest == null
                                || candidate.distanceBlocks() < nearest.distanceBlocks()
                                || (candidate.distanceBlocks() == nearest.distanceBlocks()
                                && candidate.stationBlocks() < nearest.stationBlocks())) {
                            nearest = candidate;
                        }
                    }
                }
            }
            return nearest;
        }

        private RouteProximity proximity(int segment, PlanarPoint point) {
            RampCenterlineSample start = samples.get(segment);
            RampCenterlineSample end = samples.get(segment + 1);
            double segmentX = end.point().x() - start.point().x();
            double segmentZ = end.point().z() - start.point().z();
            double lengthSquared = segmentX * segmentX + segmentZ * segmentZ;
            double fraction = lengthSquared == 0.0
                    ? 0.0
                    : ((point.x() - start.point().x()) * segmentX
                    + (point.z() - start.point().z()) * segmentZ) / lengthSquared;
            fraction = StrictMath.max(0.0, StrictMath.min(1.0, fraction));
            double projectedX = start.point().x() + segmentX * fraction;
            double projectedZ = start.point().z() + segmentZ * fraction;
            return new RouteProximity(
                    start.stationBlocks()
                            + (end.stationBlocks() - start.stationBlocks()) * fraction,
                    StrictMath.hypot(
                            projectedX - point.x(), projectedZ - point.z()));
        }

        private static int cell(double coordinate) {
            return (int) StrictMath.floor(coordinate / CELL_BLOCKS);
        }

        private static long key(int cellX, int cellZ) {
            return ((long) cellX << 32) ^ (cellZ & 0xffff_ffffL);
        }
    }

    private record TangentLengths(
            double departureBlocks,
            double arrivalBlocks) {
    }
}
