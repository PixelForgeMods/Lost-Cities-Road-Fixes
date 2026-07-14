package net.austizz.lostcitiesroadfixes.interchange.render;

import net.austizz.lostcitiesroadfixes.interchange.layout.ApproachDirection;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeGeometrySite;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeLayout;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeLayoutFactory;
import net.austizz.lostcitiesroadfixes.interchange.geometry.PlanarPoint;
import net.austizz.lostcitiesroadfixes.interchange.planning.CrossingDecks;
import net.austizz.lostcitiesroadfixes.interchange.planning.DetectedRoadCrossing;
import net.austizz.lostcitiesroadfixes.interchange.planning.PlannedInterchange;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InterchangeGeometryPlanner {
    private final InterchangeLayoutFactory layoutFactory;

    public InterchangeGeometryPlanner(InterchangeLayoutFactory layoutFactory) {
        this.layoutFactory = Objects.requireNonNull(layoutFactory, "layoutFactory");
    }

    public PlannedInterchangeGeometry create(PlannedInterchange plan) {
        Objects.requireNonNull(plan, "plan");
        DetectedRoadCrossing crossing = plan.crossing();
        CrossingDecks decks = crossing.decks();
        int centerX = Math.addExact(Math.multiplyExact(crossing.chunk().x(), 16), 8);
        int centerZ = Math.addExact(Math.multiplyExact(crossing.chunk().z(), 16), 8);
        InterchangeGeometrySite site = new InterchangeGeometrySite(
                new PlanarPoint(centerX, centerZ),
                crossing.approachRunBlocks(),
                decks.nativeX(),
                decks.nativeZ(),
                decks.plannedX(),
                decks.plannedZ());
        InterchangeLayout layout = layoutFactory.create(
                plan.decision().selected().orElseThrow(), site, crossing.approaches());

        List<GradedArterial> arterials = new ArrayList<>(2);
        if (crossing.approaches().contains(ApproachDirection.WEST)
                || crossing.approaches().contains(ApproachDirection.EAST)) {
            arterials.add(new GradedArterial(
                    RoadAxis.X,
                    centerX,
                    centerZ,
                    crossing.approachRunBlocks(),
                    crossing.approaches().contains(ApproachDirection.WEST),
                    crossing.approaches().contains(ApproachDirection.EAST),
                    decks.nativeX(),
                    decks.plannedX()));
        }
        if (crossing.approaches().contains(ApproachDirection.NORTH)
                || crossing.approaches().contains(ApproachDirection.SOUTH)) {
            arterials.add(new GradedArterial(
                    RoadAxis.Z,
                    centerX,
                    centerZ,
                    crossing.approachRunBlocks(),
                    crossing.approaches().contains(ApproachDirection.NORTH),
                    crossing.approaches().contains(ApproachDirection.SOUTH),
                    decks.nativeZ(),
                    decks.plannedZ()));
        }
        return new PlannedInterchangeGeometry(plan, layout, arterials);
    }
}
