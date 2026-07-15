package net.austizz.lostcitiesroadfixes.render;

import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoadClearanceEnvelopePlannerTest {
    @Test
    void risesFromBothShouldersIntoATallRoundedCrown() {
        ChunkPoint chunk = new ChunkPoint(0, 0);
        HalfBlockElevation elevation = new HalfBlockElevation(140);
        List<RoadSurfaceCell> cells = new ArrayList<>();
        for (int x = 0; x < 7; x++) {
            RoadSurfaceRole role = x == 0 || x == 6
                    ? RoadSurfaceRole.SHOULDER
                    : RoadSurfaceRole.ASPHALT;
            cells.add(new RoadSurfaceCell(
                    new RoadSurfacePosition(x, 8, elevation), role));
        }

        var envelope = new RoadClearanceEnvelopePlanner().plan(
                new ChunkRoadSurface(chunk, cells));

        assertEquals(List.of(8, 11, 12, 12, 12, 11, 8),
                cells.stream().map(cell -> envelope.get(cell.position())).toList());
    }

    @Test
    void defaultsToMaximumClearanceWhenAChunkContainsNoShoulder() {
        ChunkPoint chunk = new ChunkPoint(0, 0);
        RoadSurfacePosition position = new RoadSurfacePosition(
                8, 8, new HalfBlockElevation(140));
        ChunkRoadSurface surface = new ChunkRoadSurface(chunk, List.of(
                new RoadSurfaceCell(position, RoadSurfaceRole.ASPHALT)));

        assertEquals(
                RoadClearanceEnvelopePlanner.MAXIMUM_HEADROOM_BLOCKS,
                new RoadClearanceEnvelopePlanner().plan(surface).get(position));
    }
}
