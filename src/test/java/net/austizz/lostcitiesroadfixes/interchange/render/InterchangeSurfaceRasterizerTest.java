package net.austizz.lostcitiesroadfixes.interchange.render;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDecision;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeSelector;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.TrafficDemand;
import net.austizz.lostcitiesroadfixes.interchange.layout.ApproachDirection;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeConnection;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeLayoutFactory;
import net.austizz.lostcitiesroadfixes.interchange.layout.MovementKind;
import net.austizz.lostcitiesroadfixes.interchange.layout.RampForm;
import net.austizz.lostcitiesroadfixes.interchange.planning.CrossingDecks;
import net.austizz.lostcitiesroadfixes.interchange.planning.DetectedRoadCrossing;
import net.austizz.lostcitiesroadfixes.interchange.planning.PlannedInterchange;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
import net.austizz.lostcitiesroadfixes.render.ChunkRoadSurface;
import net.austizz.lostcitiesroadfixes.render.ElevatedRoadTile;
import net.austizz.lostcitiesroadfixes.render.MinecraftRoadWriter;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceCell;
import net.austizz.lostcitiesroadfixes.render.RoadSurfacePosition;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRole;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterchangeSurfaceRasterizerTest {
    private static final RoadDesignStandard STANDARD = RoadDesignStandard.DEFAULT;
    private final InterchangeGeometryPlanner geometryPlanner = new InterchangeGeometryPlanner(
            new InterchangeLayoutFactory(STANDARD));
    private final InterchangeSurfaceRasterizer rasterizer = new InterchangeSurfaceRasterizer();

    @Test
    void keepsNativePortsButRaisesTheUpperMainlineAtTheCenter() {
        PlannedInterchangeGeometry geometry = geometryPlanner.create(selected(false));
        InterchangeConnection northboundMainline = geometry.layout().connections().stream()
                .filter(connection -> connection.movement().from() == ApproachDirection.SOUTH)
                .filter(connection -> connection.movement().kind() == MovementKind.STRAIGHT)
                .findFirst()
                .orElseThrow();

        assertEquals(elevation(152), northboundMainline.route().centerline().startElevation());
        assertEquals(elevation(152), northboundMainline.route().centerline().endElevation());
        assertEquals(elevation(160), northboundMainline.route().centerline().elevationAt(
                northboundMainline.route().centerline().lengthBlocks() / 2.0));
        assertEquals(elevation(140), geometry.layout().site().xRoadCenterElevation());
        assertEquals(elevation(160), geometry.layout().site().zRoadCenterElevation());
    }

    @Test
    void rendersTwoFullWidthDecksAtTenBlockCenterSeparation() {
        PlannedInterchangeGeometry geometry = geometryPlanner.create(selected(false));
        ChunkRoadSurface center = rasterizer.rasterize(
                new ChunkPoint(0, 0), List.of(geometry));

        assertTrue(center.cellAt(8, 8, elevation(140)).isPresent());
        assertTrue(center.cellAt(8, 8, elevation(160)).isPresent());
        assertFalse(center.cellAt(8, 8, elevation(152)).isPresent());

        long width = List.of(new ChunkPoint(0, -1), new ChunkPoint(0, 0), new ChunkPoint(0, 1))
                .stream()
                .map(chunk -> rasterizer.rasterize(chunk, List.of(geometry)))
                .flatMap(surface -> surface.cells().stream())
                .filter(cell -> cell.position().x() == 8)
                .filter(cell -> cell.position().elevation().equals(elevation(140)))
                .count();
        assertEquals(32, width);
    }

    @Test
    void returnsToNativeElevationAtTheEnvelopeAndKeepsChunkSeamsClosed() {
        PlannedInterchangeGeometry geometry = geometryPlanner.create(selected(false));
        ChunkRoadSurface beforeEndpoint = rasterizer.rasterize(
                new ChunkPoint(0, 15), List.of(geometry));
        ChunkRoadSurface endpoint = rasterizer.rasterize(
                new ChunkPoint(0, 16), List.of(geometry));

        assertTrue(beforeEndpoint.cellAt(8, 255, elevation(153)).isPresent()
                || beforeEndpoint.cellAt(8, 255, elevation(152)).isPresent());
        assertTrue(endpoint.cellAt(8, 264, elevation(152)).isPresent());
    }

    @Test
    void clearsConflictingDeckCellsFromReportedEighteenBlockRampMerge() {
        PlannedInterchangeGeometry geometry = geometryPlanner.create(selected(
                elevation(140), elevation(176), elevation(140), elevation(176)));
        ChunkRoadSurface merge = rasterizer.rasterize(
                new ChunkPoint(0, 15), List.of(geometry));

        Map<String, List<RoadSurfaceCell>> byColumn = merge.cells().stream()
                .collect(Collectors.groupingBy(cell ->
                        cell.position().x() + "," + cell.position().z()));
        List<String> conflicts = byColumn.entrySet().stream()
                .filter(entry -> hasVerticalConflict(entry.getValue()))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        assertTrue(conflicts.isEmpty(), () -> "vertically conflicting road columns: " + conflicts);
    }

    @Test
    void aThreeWayGeometryOmitsItsMissingNorthArm() {
        PlannedInterchangeGeometry geometry = geometryPlanner.create(selected(true));
        GradedArterial zRoad = geometry.arterials().stream()
                .filter(arterial -> arterial.axis() == RoadAxis.Z)
                .findFirst()
                .orElseThrow();

        assertFalse(zRoad.negativeArm());
        assertTrue(zRoad.positiveArm());
        ChunkRoadSurface north = new GradedArterialRasterizer().rasterize(
                new ChunkPoint(0, -1), geometry.arterials());
        ChunkRoadSurface center = new GradedArterialRasterizer().rasterize(
                new ChunkPoint(0, 0), geometry.arterials());
        assertTrue(north.cells().stream().noneMatch(cell ->
                cell.position().elevation().equals(elevation(160))));
        assertTrue(center.cellAt(8, 8, elevation(160)).isPresent());
    }

    @Test
    void rotatesAThreeWayLayoutToEverySurveyedOrientation() {
        for (ApproachDirection missing : ApproachDirection.values()) {
            EnumSet<ApproachDirection> surveyed = EnumSet.allOf(ApproachDirection.class);
            surveyed.remove(missing);

            PlannedInterchangeGeometry geometry = geometryPlanner.create(selected(surveyed));

            assertEquals(surveyed, geometry.layout().approaches());
            assertTrue(geometry.layout().connections().stream().allMatch(connection ->
                    surveyed.contains(connection.movement().from())
                            && surveyed.contains(connection.movement().to())));
            assertEquals(6, geometry.layout().connections().size());
        }
    }

    @Test
    void mergesRolesWithoutFlatteningStackedCells() {
        ChunkPoint chunk = new ChunkPoint(0, 0);
        RoadSurfacePosition lower = new RoadSurfacePosition(8, 8, elevation(140));
        RoadSurfacePosition upper = new RoadSurfacePosition(8, 8, elevation(160));
        ChunkRoadSurface first = new ChunkRoadSurface(chunk, List.of(
                new RoadSurfaceCell(lower, RoadSurfaceRole.SHOULDER),
                new RoadSurfaceCell(upper, RoadSurfaceRole.ASPHALT)));
        ChunkRoadSurface second = new ChunkRoadSurface(chunk, List.of(
                new RoadSurfaceCell(lower, RoadSurfaceRole.ASPHALT)));

        ChunkRoadSurface merged = new ChunkRoadSurfaceMerger().merge(
                chunk, List.of(first, second));

        assertEquals(RoadSurfaceRole.ASPHALT, merged.cellAt(8, 8, elevation(140))
                .orElseThrow().role());
        assertTrue(merged.cellAt(8, 8, elevation(160)).isPresent());
        assertEquals(2, merged.cells().size());
    }

    @Test
    void identifiesOnlyNativeTilesCoveredByItsSurveyedArms() {
        PlannedInterchangeGeometry fourWay = geometryPlanner.create(selected(false));
        PlannedInterchangeGeometry threeWay = geometryPlanner.create(selected(true));

        assertTrue(fourWay.replaces(tile(new ChunkPoint(15, 0), RoadAxis.X, 140)));
        assertFalse(fourWay.replaces(tile(new ChunkPoint(17, 0), RoadAxis.X, 140)));
        assertFalse(fourWay.replaces(tile(new ChunkPoint(15, 1), RoadAxis.X, 140)));
        assertFalse(threeWay.replaces(tile(new ChunkPoint(0, -1), RoadAxis.Z, 152)));
        assertTrue(threeWay.replaces(tile(new ChunkPoint(0, 1), RoadAxis.Z, 152)));
    }

    private static PlannedInterchange selected(boolean threeWay) {
        return selected(threeWay
                ? EnumSet.of(
                        ApproachDirection.WEST,
                        ApproachDirection.EAST,
                        ApproachDirection.SOUTH)
                : EnumSet.allOf(ApproachDirection.class));
    }

    private static PlannedInterchange selected(Set<ApproachDirection> approaches) {
        boolean threeWay = approaches.size() == 3;
        DetectedRoadCrossing crossing = new DetectedRoadCrossing(
                new ChunkPoint(0, 0),
                threeWay ? JunctionForm.THREE_WAY : JunctionForm.FOUR_WAY,
                0,
                1,
                approaches,
                256,
                128,
                threeWay ? 3 : 4,
                TrafficDemand.HIGH,
                4,
                true,
                true,
                new CrossingDecks(
                        elevation(140), elevation(152), elevation(140), elevation(160)),
                0x5eedL);
        InterchangeDecision decision = InterchangeSelector.withBuiltIns()
                .select(crossing.selectionSite());
        return new PlannedInterchange(crossing, decision);
    }

    private static PlannedInterchange selected(
            HalfBlockElevation nativeX,
            HalfBlockElevation nativeZ,
            HalfBlockElevation plannedX,
            HalfBlockElevation plannedZ) {
        DetectedRoadCrossing crossing = new DetectedRoadCrossing(
                new ChunkPoint(0, 0),
                JunctionForm.FOUR_WAY,
                0,
                3,
                EnumSet.allOf(ApproachDirection.class),
                256,
                128,
                4,
                TrafficDemand.HIGH,
                4,
                true,
                true,
                new CrossingDecks(nativeX, nativeZ, plannedX, plannedZ),
                0x5eedL);
        InterchangeDecision decision = InterchangeSelector.withBuiltIns()
                .select(crossing.selectionSite());
        return new PlannedInterchange(crossing, decision);
    }

    private static boolean hasVerticalConflict(List<RoadSurfaceCell> column) {
        int clearanceHalfBlocks = MinecraftRoadWriter.HEADROOM_BLOCKS * 2;
        for (int left = 0; left < column.size(); left++) {
            for (int right = left + 1; right < column.size(); right++) {
                int delta = StrictMath.abs(
                        column.get(left).position().elevation().halfBlocks()
                                - column.get(right).position().elevation().halfBlocks());
                if (delta <= clearanceHalfBlocks) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ElevatedRoadTile tile(ChunkPoint chunk, RoadAxis axis, int halfBlocks) {
        return new ElevatedRoadTile(chunk, axis, elevation(halfBlocks));
    }

    private static HalfBlockElevation elevation(int halfBlocks) {
        return new HalfBlockElevation(halfBlocks);
    }
}
