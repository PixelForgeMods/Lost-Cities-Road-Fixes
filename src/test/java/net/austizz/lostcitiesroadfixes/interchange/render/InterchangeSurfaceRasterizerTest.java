package net.austizz.lostcitiesroadfixes.interchange.render;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDecision;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeCatalogue;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesign;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeSelector;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeType;
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
import net.austizz.lostcitiesroadfixes.integration.RuntimeRoadSurfaceComposer;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
import net.austizz.lostcitiesroadfixes.render.ChunkRoadSurface;
import net.austizz.lostcitiesroadfixes.render.ElevatedRoadTile;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceCell;
import net.austizz.lostcitiesroadfixes.render.RoadSurfacePosition;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRole;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    void rendersTwoDecksAndTheirContinuousAuxiliaryLanes() {
        PlannedInterchangeGeometry geometry = geometryPlanner.create(selected(false));
        ChunkRoadSurface center = rasterizer.rasterize(
                new ChunkPoint(0, 0), List.of(geometry));

        assertTrue(center.cellAt(8, 8, elevation(140)).isPresent());
        assertTrue(center.cellAt(8, 8, elevation(160)).isPresent(), () ->
                center.cells().stream()
                        .filter(cell -> cell.position().x() == 8
                                && cell.position().z() == 8)
                        .toList().toString());
        assertFalse(center.cellAt(8, 8, elevation(152)).isPresent());

        long width = List.of(new ChunkPoint(0, -1), new ChunkPoint(0, 0), new ChunkPoint(0, 1))
                .stream()
                .map(chunk -> rasterizer.rasterize(chunk, List.of(geometry)))
                .flatMap(surface -> surface.cells().stream())
                .filter(cell -> cell.position().x() == 8)
                .filter(cell -> cell.position().elevation().equals(elevation(140)))
                .count();
        assertEquals(48, width);
    }

    @Test
    void returnsToNativeElevationAtTheEnvelopeAndKeepsChunkSeamsClosed() {
        PlannedInterchangeGeometry geometry = geometryPlanner.create(selected(false));
        ChunkRoadSurface beforeEndpoint = rasterizer.rasterize(
                new ChunkPoint(0, 19), List.of(geometry));
        ChunkRoadSurface endpoint = rasterizer.rasterize(
                new ChunkPoint(0, 20), List.of(geometry));

        assertTrue(beforeEndpoint.cellAt(8, 319, elevation(153)).isPresent()
                || beforeEndpoint.cellAt(8, 319, elevation(152)).isPresent());
        assertTrue(endpoint.cellAt(8, 328, elevation(152)).isPresent());
    }

    @Test
    void auxiliaryLaneUsesAContinuousTaperBeforeTheFirstRampTerminal() {
        PlannedInterchangeGeometry geometry = geometryPlanner.create(selected(false));
        ChunkRoadSurface start = rasterizer.rasterize(
                new ChunkPoint(-20, 1), List.of(geometry));
        ChunkRoadSurface middle = rasterizer.rasterize(
                new ChunkPoint(-19, 1), List.of(geometry));
        ChunkRoadSurface fullWidth = rasterizer.rasterize(
                new ChunkPoint(-18, 1), List.of(geometry));

        assertFalse(start.cellAt(-312, 25, elevation(140)).isPresent());
        assertTrue(middle.cellAt(-296, 25, elevation(140)).isPresent());
        assertTrue(fullWidth.cellAt(-280, 25, elevation(140)).isPresent());
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
                cell.position().elevation().equals(elevation(168))));
        assertTrue(center.cellAt(8, 8, elevation(168)).isPresent());
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

        assertTrue(fourWay.replaces(tile(new ChunkPoint(19, 0), RoadAxis.X, 140)));
        assertFalse(fourWay.replaces(tile(new ChunkPoint(21, 0), RoadAxis.X, 140)));
        assertFalse(fourWay.replaces(tile(new ChunkPoint(19, 1), RoadAxis.X, 140)));
        assertFalse(threeWay.replaces(tile(new ChunkPoint(0, -1), RoadAxis.Z, 152)));
        assertTrue(threeWay.replaces(tile(new ChunkPoint(0, 1), RoadAxis.Z, 152)));
    }

    @Test
    void everyBuiltInComposesSafelyAcrossItsFullCompiledEnvelope() {
        RuntimeRoadSurfaceComposer composer = new RuntimeRoadSurfaceComposer();
        for (InterchangeDesign design : InterchangeCatalogue.builtIns()) {
            int upperHalfBlocks = switch (design.type()) {
                case STACK -> 188;
                case THREE_WAY_DIRECTIONAL -> 168;
                default -> 160;
            };
            Set<ApproachDirection> approaches = design.form() == JunctionForm.THREE_WAY
                    ? EnumSet.of(
                            ApproachDirection.WEST,
                            ApproachDirection.EAST,
                            ApproachDirection.SOUTH)
                    : EnumSet.allOf(ApproachDirection.class);
            CrossingDecks decks = new CrossingDecks(
                    elevation(140),
                    elevation(upperHalfBlocks),
                    elevation(140),
                    elevation(upperHalfBlocks));
            DetectedRoadCrossing crossing = new DetectedRoadCrossing(
                    new ChunkPoint(0, 0),
                    design.form(),
                    0,
                    design.type() == InterchangeType.STACK ? 4 : 1,
                    approaches,
                    640,
                    512,
                    4,
                    TrafficDemand.LOCAL,
                    4,
                    true,
                    false,
                    decks,
                    1L);
            InterchangeDecision decision = new InterchangeSelector(
                    List.of(design), STANDARD).select(crossing.selectionSite());
            assertEquals(
                    design,
                    decision.selected().orElseThrow(() -> new AssertionError(
                            design.type() + ": " + decision.diagnostic())),
                    decision::diagnostic);
            PlannedInterchangeGeometry geometry = geometryPlanner.create(
                    new PlannedInterchange(crossing, decision));
            int chunkRadius = Math.floorDiv(
                    decision.selectedApproachRunBlocks() + 15, 16);

            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                for (int x = -chunkRadius; x <= chunkRadius; x++) {
                    ChunkPoint chunk = new ChunkPoint(x, z);
                    assertDoesNotThrow(
                            () -> composer.compose(
                                    chunk,
                                    List.of(),
                                    List.of(geometry)),
                            () -> design.type() + " failed in " + chunk);
                }
            }

            Map<ChunkPoint, ChunkRoadSurface> routeSurfaces = new HashMap<>();
            Map<net.austizz.lostcitiesroadfixes.interchange.geometry.RampRoute, String>
                    routeLabels = new HashMap<>();
            geometry.layout().connections().forEach(connection -> routeLabels.put(
                    connection.route(), connection.movement() + " " + connection.form()));
            geometry.layout().auxiliaryLanes().forEach(lane -> routeLabels.put(
                    lane.route(), lane.mainlineMovement() + " AUXILIARY"));
            geometry.rampAndAuxiliaryRoutes().forEach(route -> {
                String routeLabel = routeLabels.getOrDefault(route, "unknown route");
                route.centerline().samples().forEach(sample -> {
                        int blockX = (int) StrictMath.floor(sample.point().x());
                        int blockZ = (int) StrictMath.floor(sample.point().z());
                        ChunkPoint chunk = new ChunkPoint(
                                Math.floorDiv(blockX, 16),
                                Math.floorDiv(blockZ, 16));
                        ChunkRoadSurface surface = routeSurfaces.computeIfAbsent(
                                chunk,
                                ignored -> composer.compose(
                                        chunk,
                                        List.of(),
                                        List.of(geometry)));
                        int nearestElevationDifference = surface.cells().stream()
                                .filter(cell -> cell.position().x() == blockX)
                                .filter(cell -> cell.position().z() == blockZ)
                                .mapToInt(cell -> StrictMath.abs(
                                        cell.position().elevation().halfBlocks()
                                                - sample.elevation().halfBlocks()))
                                .min()
                                .orElse(Integer.MAX_VALUE);
                        assertTrue(
                                nearestElevationDifference
                                        < STANDARD.minimumVehicleClearanceBlocks() * 2,
                                () -> design.type() + " lost route centerline near "
                                        + blockX + ',' + blockZ + " at "
                                        + sample.elevation().halfBlocks()
                                        + " half-blocks on " + routeLabel
                                        + "; nearest surface differs by "
                                        + nearestElevationDifference);
                });
            });
        }
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
                320,
                240,
                threeWay ? 3 : 4,
                TrafficDemand.HIGH,
                4,
                true,
                true,
                new CrossingDecks(
                        elevation(140), elevation(152), elevation(140),
                        threeWay ? elevation(168) : elevation(160)),
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
                512,
                256,
                4,
                TrafficDemand.HIGH,
                4,
                true,
                false,
                new CrossingDecks(nativeX, nativeZ, plannedX, plannedZ),
                0x5eedL);
        InterchangeDecision decision = InterchangeSelector.withBuiltIns()
                .select(crossing.selectionSite());
        return new PlannedInterchange(crossing, decision);
    }

    private static boolean hasVerticalConflict(List<RoadSurfaceCell> column) {
        int clearanceHalfBlocks = STANDARD.minimumVehicleClearanceBlocks() * 2;
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
