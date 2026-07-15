package net.austizz.lostcitiesroadfixes.integration;

import net.austizz.lostcitiesroadfixes.interchange.render.InterchangeSurfaceRasterizer;
import net.austizz.lostcitiesroadfixes.interchange.render.PlannedInterchangeGeometry;
import net.austizz.lostcitiesroadfixes.interchange.render.VerticalClearanceSurfaceMerger;
import net.austizz.lostcitiesroadfixes.render.ChunkRoadSurface;
import net.austizz.lostcitiesroadfixes.render.ElevatedRoadTile;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRasterizer;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceClearanceValidator;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class RuntimeRoadSurfaceComposer {
    private final RoadSurfaceRasterizer roadRasterizer = new RoadSurfaceRasterizer();
    private final InterchangeSurfaceRasterizer interchangeRasterizer =
            new InterchangeSurfaceRasterizer();
    private final VerticalClearanceSurfaceMerger protectedInterchangeMerger =
            new VerticalClearanceSurfaceMerger(
                    RoadDesignStandard.DEFAULT.minimumVehicleClearanceBlocks());
    private final RoadSurfaceClearanceValidator clearanceValidator =
            new RoadSurfaceClearanceValidator(
                    RoadDesignStandard.DEFAULT.minimumVehicleClearanceBlocks());

    public ChunkRoadSurface compose(
            ChunkPoint targetChunk,
            Collection<ElevatedRoadTile> nativeRoads,
            Collection<PlannedInterchangeGeometry> interchanges) {
        Objects.requireNonNull(targetChunk, "targetChunk");
        Objects.requireNonNull(nativeRoads, "nativeRoads");
        Objects.requireNonNull(interchanges, "interchanges");
        List<PlannedInterchangeGeometry> geometry = List.copyOf(interchanges);
        ChunkRoadSurface nativeSurface = roadRasterizer.rasterize(targetChunk, nativeRoads);
        ChunkRoadSurface unaffectedSurface = new ChunkRoadSurface(
                targetChunk,
                nativeSurface.cells().stream()
                        .filter(cell -> geometry.stream().noneMatch(interchange ->
                                interchange.replacesNativeCell(cell.position())))
                        .toList());
        ChunkRoadSurface interchangeSurface = interchangeRasterizer.rasterize(
                targetChunk, geometry);
        ChunkRoadSurface composed = protectedInterchangeMerger.merge(
                targetChunk, unaffectedSurface, interchangeSurface);
        clearanceValidator.requireSafe(composed);
        return composed;
    }
}
