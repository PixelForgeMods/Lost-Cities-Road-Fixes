package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.interchange.geometry.PlanarPoint;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeGeometrySite;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeConnection;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeLayout;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeLayoutFactory;
import net.austizz.lostcitiesroadfixes.interchange.layout.MovementKind;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterchangeRouteMetricsTest {
    @Test
    void shortestMetricMatchesTheCompiledRightTurnForAnEighteenBlockRise() {
        InterchangeDesign cloverleaf = InterchangeCatalogue.builtIns().stream()
                .filter(design -> design.type() == InterchangeType.CLOVERLEAF)
                .findFirst()
                .orElseThrow();
        InterchangeGeometrySite site = new InterchangeGeometrySite(
                new PlanarPoint(8.0, 8.0),
                256,
                new HalfBlockElevation(140),
                new HalfBlockElevation(176));

        InterchangeLayout layout = new InterchangeLayoutFactory(RoadDesignStandard.DEFAULT)
                .create(cloverleaf, site);
        InterchangeConnection rightTurn = layout.connections().stream()
                .filter(connection -> connection.movement().kind() == MovementKind.RIGHT)
                .findFirst()
                .orElseThrow();
        int compiledPreMergeRun = (int) StrictMath.floor(
                rightTurn.route().centerline().lengthBlocks()
                        - InterchangeRouteMetrics.directTangentBlocks(
                                cloverleaf, site.approachRunBlocks())
                        + 1.0e-9);
        int measuredShortest = InterchangeRouteMetrics.shortestTurningRouteRunBlocks(
                cloverleaf, site.approachRunBlocks());

        assertEquals(307, measuredShortest);
        assertEquals(compiledPreMergeRun, measuredShortest);
        assertTrue(measuredShortest >= 288);
        assertEquals(new HalfBlockElevation(176),
                rightTurn.route().centerline().elevationAt(288.0));
        assertEquals(new HalfBlockElevation(176),
                rightTurn.route().centerline().elevationAt(
                        rightTurn.route().centerline().lengthBlocks() - 1.0));
    }
}
