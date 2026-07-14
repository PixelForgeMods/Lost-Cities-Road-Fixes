package net.austizz.lostcitiesroadfixes.integration;

import net.austizz.lostcitiesroadfixes.interchange.render.ChunkRoadSurfaceMerger;
import net.austizz.lostcitiesroadfixes.interchange.render.InterchangeSurfaceRasterizer;
import net.austizz.lostcitiesroadfixes.interchange.render.PlannedInterchangeGeometry;
import net.austizz.lostcitiesroadfixes.render.ChunkRoadSurface;
import net.austizz.lostcitiesroadfixes.render.ElevatedRoadTile;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRasterizer;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class RuntimeRoadSurfaceComposer {
    private final RoadSurfaceRasterizer roadRasterizer = new RoadSurfaceRasterizer();
    private final InterchangeSurfaceRasterizer interchangeRasterizer =
            new InterchangeSurfaceRasterizer();
    private final ChunkRoadSurfaceMerger merger = new ChunkRoadSurfaceMerger();

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
        return merger.merge(targetChunk, List.of(
                unaffectedSurface,
                interchangeRasterizer.rasterize(targetChunk, geometry)));
    }
}
