package net.austizz.lostcitiesroadfixes.interchange.render;

import net.austizz.lostcitiesroadfixes.interchange.geometry.RampRoute;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeLayout;
import net.austizz.lostcitiesroadfixes.interchange.layout.RampForm;
import net.austizz.lostcitiesroadfixes.interchange.planning.PlannedInterchange;
import net.austizz.lostcitiesroadfixes.render.ElevatedRoadTile;
import net.austizz.lostcitiesroadfixes.render.RoadSurfacePosition;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.List;
import java.util.Objects;

public record PlannedInterchangeGeometry(
        PlannedInterchange plan,
        InterchangeLayout layout,
        List<GradedArterial> arterials) {
    public PlannedInterchangeGeometry {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(layout, "layout");
        arterials = List.copyOf(arterials);
        if (!layout.design().equals(plan.decision().selected().orElseThrow())) {
            throw new IllegalArgumentException("Layout does not use the selected interchange design");
        }
    }

    public List<RampRoute> turningRoutes() {
        return layout.connections().stream()
                .filter(connection -> connection.form() != RampForm.MAINLINE)
                .map(connection -> connection.route())
                .toList();
    }

    public boolean replaces(ElevatedRoadTile road) {
        return arterials.stream().anyMatch(arterial -> arterial.replaces(road));
    }

    public boolean replacesNativeCell(RoadSurfacePosition position) {
        return arterials.stream().anyMatch(arterial -> arterial.replacesNativeCell(position));
    }

    public boolean mayAffect(ChunkPoint target) {
        Objects.requireNonNull(target, "target");
        ChunkPoint center = plan.crossing().chunk();
        int chunkRadius = Math.floorDiv(
                Math.addExact(plan.crossing().approachRunBlocks(), 15), 16);
        return StrictMath.abs((long) target.x() - center.x()) <= chunkRadius
                && StrictMath.abs((long) target.z() - center.z()) <= chunkRadius;
    }
}
