package net.austizz.lostcitiesroadfixes.render;

import net.austizz.lostcitiesroadfixes.interchange.geometry.PlanarPoint;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampCenterline;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampPathBuilder;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampRoute;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RoadHeading;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RampSurfaceRasterizerTest {
    private final RampSurfaceRasterizer rasterizer = new RampSurfaceRasterizer();

    @Test
    void keepsAContinuousSurfaceAcrossChunkSeams() {
        RampRoute route = levelRoute(-8.0, 8.0, 48, 140);

        ChunkRoadSurface west = rasterizer.rasterize(new ChunkPoint(0, 0), List.of(route));
        ChunkRoadSurface east = rasterizer.rasterize(new ChunkPoint(1, 0), List.of(route));

        assertTrue(west.cellAt(15, 8, elevation(140)).isPresent());
        assertTrue(east.cellAt(16, 8, elevation(140)).isPresent());
        assertFalse(west.cells().isEmpty());
        assertFalse(east.cells().isEmpty());
        assertEquals(8, west.cells().stream()
                .filter(cell -> cell.position().x() == 8)
                .count(), "an eight-block ramp must remain exactly eight blocks wide");
    }

    @Test
    void preservesStackedRoutesAtDifferentElevations() {
        RampRoute lower = levelRoute(0.0, 8.0, 16, 140);
        RampRoute upper = levelRoute(0.0, 8.0, 16, 160);

        ChunkRoadSurface surface = rasterizer.rasterize(
                new ChunkPoint(0, 0), List.of(lower, upper));

        assertTrue(surface.cellAt(8, 8, elevation(140)).isPresent());
        assertTrue(surface.cellAt(8, 8, elevation(160)).isPresent());
    }

    @Test
    void rasterizationIsDeterministicAndChunkBounded() {
        RampCenterline centerline = new RampPathBuilder(
                RoadDesignStandard.DEFAULT,
                new PlanarPoint(-8.0, 12.5),
                RoadHeading.EAST)
                .straight(24)
                .turnLeft(24, 90)
                .straight(48)
                .build(elevation(120), elevation(128));
        RampRoute route = new RampRoute(centerline, 8);
        ChunkPoint chunk = new ChunkPoint(0, 0);

        ChunkRoadSurface first = rasterizer.rasterize(chunk, List.of(route));
        ChunkRoadSurface second = rasterizer.rasterize(chunk, List.of(route));

        assertEquals(first.cells(), second.cells());
        assertTrue(first.cells().stream().allMatch(cell ->
                cell.position().x() >= chunk.minBlockX()
                        && cell.position().x() <= chunk.maxBlockX()
                        && cell.position().z() >= chunk.minBlockZ()
                        && cell.position().z() <= chunk.maxBlockZ()));
    }

    private static RampRoute levelRoute(double x, double z, int length, int halfBlocks) {
        RampCenterline centerline = new RampPathBuilder(
                RoadDesignStandard.DEFAULT,
                new PlanarPoint(x, z),
                RoadHeading.EAST)
                .straight(length)
                .build(elevation(halfBlocks), elevation(halfBlocks));
        return new RampRoute(centerline, 8);
    }

    private static HalfBlockElevation elevation(int halfBlocks) {
        return new HalfBlockElevation(halfBlocks);
    }
}
