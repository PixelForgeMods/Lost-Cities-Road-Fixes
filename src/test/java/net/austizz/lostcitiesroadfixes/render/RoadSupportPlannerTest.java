package net.austizz.lostcitiesroadfixes.render;

import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoadSupportPlannerTest {
    private final RoadSupportPlanner planner = new RoadSupportPlanner();

    @Test
    void choosesTwoSpreadAnchorsFromTheLowestDeckDeterministically() {
        List<RoadSurfaceCell> cells = List.of(
                cell(8, 8, 140),
                cell(1, 1, 160),
                cell(14, 14, 140),
                cell(1, 1, 140),
                cell(2, 13, 140));
        ChunkRoadSurface forward = new ChunkRoadSurface(new ChunkPoint(0, 0), cells);
        List<RoadSurfaceCell> reverseCells = new ArrayList<>(cells);
        java.util.Collections.reverse(reverseCells);
        ChunkRoadSurface reverse = new ChunkRoadSurface(new ChunkPoint(0, 0), reverseCells);

        List<RoadSurfacePosition> expected = List.of(
                position(1, 1, 140),
                position(14, 14, 140));
        assertEquals(expected, planner.anchors(forward));
        assertEquals(expected, planner.anchors(reverse));
    }

    @Test
    void returnsOneAnchorWhenOnlyOneRoadColumnExists() {
        ChunkRoadSurface surface = new ChunkRoadSurface(
                new ChunkPoint(-1, -1),
                List.of(cell(-3, -4, 120), cell(-3, -4, 140)));

        assertEquals(List.of(position(-3, -4, 120)), planner.anchors(surface));
    }

    private static RoadSurfaceCell cell(int x, int z, int halfBlocks) {
        return new RoadSurfaceCell(
                position(x, z, halfBlocks), RoadSurfaceRole.ASPHALT);
    }

    private static RoadSurfacePosition position(int x, int z, int halfBlocks) {
        return new RoadSurfacePosition(x, z, new HalfBlockElevation(halfBlocks));
    }
}
