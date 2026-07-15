package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.interchange.geometry.PlanarPoint;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeGeometrySite;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeLayout;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeLayoutFactory;
import net.austizz.lostcitiesroadfixes.interchange.layout.MovementKind;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InterchangeRouteMetricsTest {
    @Test
    void cloverleafGradesOnlyAfterASeparatedRampHasDiverged() {
        InterchangeDesign cloverleaf = InterchangeCatalogue.builtIns().stream()
                .filter(design -> design.type() == InterchangeType.CLOVERLEAF)
                .findFirst()
                .orElseThrow();
        InterchangeGeometrySite site = new InterchangeGeometrySite(
                new PlanarPoint(8.0, 8.0),
                384,
                new HalfBlockElevation(140),
                new HalfBlockElevation(160));

        InterchangeLayout layout = new InterchangeLayoutFactory(RoadDesignStandard.DEFAULT)
                .create(cloverleaf, site);
        var rightTurn = layout.connections().stream()
                .filter(connection -> connection.movement().kind() == MovementKind.RIGHT)
                .findFirst()
                .orElseThrow();

        assertTrue(rightTurn.route().centerline().elevationAt(32.0)
                .equals(new HalfBlockElevation(140)));
        assertTrue(rightTurn.route().centerline().elevationAt(
                        rightTurn.route().centerline().lengthBlocks() - 32.0)
                .equals(new HalfBlockElevation(160)));
    }

    @Test
    void rejectsCloverleafWhenTheGradeWouldForceLoopsThroughOuterRamps() {
        InterchangeSite site = new InterchangeSite(
                JunctionForm.FOUR_WAY,
                256,
                4,
                384,
                new HalfBlockElevation(140),
                new HalfBlockElevation(176),
                TrafficDemand.HIGH,
                2,
                true,
                true,
                42L);

        InterchangeEvaluation cloverleaf = InterchangeSelector.withBuiltIns()
                .select(site).evaluations().stream()
                .filter(evaluation -> evaluation.design().type() == InterchangeType.CLOVERLEAF)
                .findFirst()
                .orElseThrow();

        assertTrue(cloverleaf.rejectionReasons().stream()
                .anyMatch(reason -> reason.contains("cannot be separated")));
    }
}
