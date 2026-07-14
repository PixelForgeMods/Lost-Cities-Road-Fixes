package net.austizz.lostcitiesroadfixes.interchange.layout;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesign;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeType;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampCenterline;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampPathBuilder;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampRoute;
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
    private static final double LANE_OFFSET_BLOCKS = 8.0;
    private static final List<ApproachDirection> CLOCKWISE_APPROACHES = List.of(
            ApproachDirection.WEST,
            ApproachDirection.NORTH,
            ApproachDirection.EAST,
            ApproachDirection.SOUTH);

    private final RoadDesignStandard standard;

    public InterchangeLayoutFactory(RoadDesignStandard standard) {
        this.standard = Objects.requireNonNull(standard, "standard");
    }

    public InterchangeLayout create(
            InterchangeDesign design,
            InterchangeGeometrySite site) {
        Objects.requireNonNull(design, "design");
        Objects.requireNonNull(site, "site");
        if (site.approachRunBlocks() < design.minimumApproachRunBlocks()) {
            throw new IllegalArgumentException(
                    design.id() + " requires " + design.minimumApproachRunBlocks()
                            + " approach blocks but the site has " + site.approachRunBlocks());
        }
        if (site.approachRunBlocks() <= design.minimumRadiusBlocks()) {
            throw new IllegalArgumentException(
                    "Approach run must extend beyond the interchange radius for " + design.id());
        }

        Set<ApproachDirection> approaches = design.form() == JunctionForm.THREE_WAY
                ? EnumSet.of(ApproachDirection.WEST, ApproachDirection.EAST, ApproachDirection.SOUTH)
                : EnumSet.allOf(ApproachDirection.class);
        List<ConnectionDraft> drafts = createDrafts(design, site, approaches);
        List<InterchangeConnection> connections = finishConnections(design, drafts);
        return new InterchangeLayout(design, site, approaches, connections);
    }

    private List<ConnectionDraft> createDrafts(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            Set<ApproachDirection> approaches) {
        List<ConnectionDraft> result = new ArrayList<>();
        for (ApproachDirection from : CLOCKWISE_APPROACHES) {
            if (!approaches.contains(from) || !approaches.contains(from.opposite())) {
                continue;
            }
            InterchangeMovement movement = new InterchangeMovement(
                    from, from.opposite(), MovementKind.STRAIGHT);
            result.add(new ConnectionDraft(
                    movement,
                    mainline(site, movement),
                    RampForm.MAINLINE));
        }
        for (ApproachDirection from : CLOCKWISE_APPROACHES) {
            if (!approaches.contains(from)) {
                continue;
            }
            addTurn(result, design, site, approaches, from, MovementKind.RIGHT);
            addTurn(result, design, site, approaches, from, MovementKind.LEFT);
        }
        return result;
    }

    private void addTurn(
            List<ConnectionDraft> result,
            InterchangeDesign design,
            InterchangeGeometrySite site,
            Set<ApproachDirection> approaches,
            ApproachDirection from,
            MovementKind kind) {
        ApproachDirection destination = kind == MovementKind.RIGHT
                ? from.rightTurnDestination()
                : from.leftTurnDestination();
        if (!approaches.contains(destination)) {
            return;
        }
        InterchangeMovement movement = new InterchangeMovement(from, destination, kind);
        boolean loop = kind == MovementKind.LEFT && usesLoop(design.type(), from);
        result.add(new ConnectionDraft(
                movement,
                loop ? loopTurn(design, site, movement) : directTurn(design, site, movement),
                loop ? RampForm.LOOP : RampForm.DIRECT));
    }

    private RampRoute mainline(InterchangeGeometrySite site, InterchangeMovement movement) {
        InterchangePort start = site.port(movement.from(), TrafficFlow.INBOUND);
        InterchangePort end = site.port(movement.to(), TrafficFlow.OUTBOUND);
        RampCenterline centerline = new RampPathBuilder(standard, start.point(), start.heading())
                .straight(site.approachRunBlocks() * 2.0)
                .build(start.elevation(), end.elevation());
        return new RampRoute(centerline, RAMP_WIDTH_BLOCKS);
    }

    private RampRoute directTurn(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            InterchangeMovement movement) {
        double radius = design.minimumRadiusBlocks();
        double tangent = site.approachRunBlocks() - radius;
        InterchangePort start = site.port(movement.from(), TrafficFlow.INBOUND);
        InterchangePort end = site.port(movement.to(), TrafficFlow.OUTBOUND);
        RampPathBuilder builder = new RampPathBuilder(standard, start.point(), start.heading())
                .straight(tangent);
        if (movement.kind() == MovementKind.RIGHT) {
            builder.turnRight(radius - LANE_OFFSET_BLOCKS, 90.0);
        } else {
            builder.turnLeft(radius + LANE_OFFSET_BLOCKS, 90.0);
        }
        RampCenterline centerline = builder
                .straight(tangent)
                .build(start.elevation(), end.elevation());
        requireEndpoint(design, movement, end, centerline);
        return new RampRoute(centerline, RAMP_WIDTH_BLOCKS);
    }

    private RampRoute loopTurn(
            InterchangeDesign design,
            InterchangeGeometrySite site,
            InterchangeMovement movement) {
        double loopRadius = (design.minimumRadiusBlocks() - LANE_OFFSET_BLOCKS) / 2.0;
        double tangent = site.approachRunBlocks() + LANE_OFFSET_BLOCKS + loopRadius;
        InterchangePort start = site.port(movement.from(), TrafficFlow.INBOUND);
        InterchangePort end = site.port(movement.to(), TrafficFlow.OUTBOUND);
        RampCenterline centerline = new RampPathBuilder(standard, start.point(), start.heading())
                .straight(tangent)
                .turnRight(loopRadius, 270.0)
                .straight(tangent)
                .build(start.elevation(), end.elevation());
        requireEndpoint(design, movement, end, centerline);
        return new RampRoute(centerline, RAMP_WIDTH_BLOCKS);
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
