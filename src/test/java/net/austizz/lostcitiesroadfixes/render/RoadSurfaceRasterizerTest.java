package net.austizz.lostcitiesroadfixes.render;

import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadSurfaceRasterizerTest {
    private final RoadSurfaceRasterizer rasterizer = new RoadSurfaceRasterizer();

    @Test
    void rasterizesTheCompleteThirtyTwoBlockCrossSectionAndRoles() {
        ElevatedRoadTile road = road(0, 0, RoadAxis.X, 142);
        List<RoadSurfaceCell> all = new ArrayList<>();
        all.addAll(rasterizer.rasterize(new ChunkPoint(0, -1), List.of(road)).cells());
        all.addAll(rasterizer.rasterize(new ChunkPoint(0, 0), List.of(road)).cells());
        all.addAll(rasterizer.rasterize(new ChunkPoint(0, 1), List.of(road)).cells());

        assertEquals(32 * 16, all.size());
        assertEquals(32, count(all, RoadSurfaceRole.SHOULDER));
        assertEquals(32, count(all, RoadSurfaceRole.MEDIAN));
        assertEquals(32, count(all, RoadSurfaceRole.YELLOW_MARKING));
        assertEquals(16, count(all, RoadSurfaceRole.WHITE_MARKING));
        assertEquals(400, count(all, RoadSurfaceRole.ASPHALT));
    }

    @Test
    void keepsDashPhaseContinuousAcrossAChunkBoundary() {
        List<ElevatedRoadTile> roads = List.of(
                road(0, 0, RoadAxis.X, 142),
                road(1, 0, RoadAxis.X, 142));
        ChunkRoadSurface west = rasterizer.rasterize(new ChunkPoint(0, -1), roads);
        ChunkRoadSurface east = rasterizer.rasterize(new ChunkPoint(1, -1), roads);

        assertEquals(RoadSurfaceRole.ASPHALT,
                west.cellAt(15, -1, new HalfBlockElevation(142)).orElseThrow().role());
        assertEquals(RoadSurfaceRole.WHITE_MARKING,
                east.cellAt(16, -1, new HalfBlockElevation(142)).orElseThrow().role());
        assertEquals(RoadSurfaceRole.WHITE_MARKING,
                east.cellAt(19, -1, new HalfBlockElevation(142)).orElseThrow().role());
        assertEquals(RoadSurfaceRole.ASPHALT,
                east.cellAt(20, -1, new HalfBlockElevation(142)).orElseThrow().role());
    }

    @Test
    void opensSameElevationCrossingsButKeepsStackedDecksSeparate() {
        ElevatedRoadTile xRoad = road(0, 0, RoadAxis.X, 142);
        ElevatedRoadTile zRoadAtGrade = road(0, 0, RoadAxis.Z, 142);
        ChunkRoadSurface atGrade = rasterizer.rasterize(
                new ChunkPoint(0, 0), List.of(xRoad, zRoadAtGrade));

        assertEquals(256, atGrade.cells().size());
        assertTrue(atGrade.cells().stream().allMatch(
                cell -> cell.role() == RoadSurfaceRole.AT_GRADE_INTERSECTION));

        ElevatedRoadTile zRoadAbove = road(0, 0, RoadAxis.Z, 162);
        ChunkRoadSurface stacked = rasterizer.rasterize(
                new ChunkPoint(0, 0), List.of(xRoad, zRoadAbove));

        assertEquals(512, stacked.cells().size());
        assertFalse(stacked.cells().stream().anyMatch(
                cell -> cell.role() == RoadSurfaceRole.AT_GRADE_INTERSECTION));
        assertEquals(256, stacked.cells().stream()
                .filter(cell -> cell.position().elevation().equals(new HalfBlockElevation(142)))
                .count());
        assertEquals(256, stacked.cells().stream()
                .filter(cell -> cell.position().elevation().equals(new HalfBlockElevation(162)))
                .count());
    }

    @Test
    void preservesHalfBlockElevationAndReturnsSortedImmutableChunkCells() {
        ChunkRoadSurface surface = rasterizer.rasterize(
                new ChunkPoint(0, 0), List.of(road(0, 0, RoadAxis.Z, 143)));

        assertEquals(256, surface.cells().size());
        assertTrue(surface.cells().stream().allMatch(
                cell -> cell.position().elevation().equals(new HalfBlockElevation(143))));
        List<RoadSurfaceCell> sorted = new ArrayList<>(surface.cells());
        sorted.sort(RoadSurfaceCell.ORDER);
        assertEquals(sorted, surface.cells());
        assertThrows(UnsupportedOperationException.class, () -> surface.cells().add(sorted.getFirst()));
    }

    private static ElevatedRoadTile road(int chunkX, int chunkZ, RoadAxis axis, int elevation) {
        return new ElevatedRoadTile(
                new ChunkPoint(chunkX, chunkZ), axis, new HalfBlockElevation(elevation));
    }

    private static long count(List<RoadSurfaceCell> cells, RoadSurfaceRole role) {
        return cells.stream().filter(cell -> cell.role() == role).count();
    }
}
