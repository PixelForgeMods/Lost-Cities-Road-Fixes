package net.austizz.lostcitiesroadfixes.integration;

import net.austizz.lostcitiesroadfixes.interchange.render.InterchangeSurfaceRasterizer;
import net.austizz.lostcitiesroadfixes.interchange.render.GradedArterial;
import net.austizz.lostcitiesroadfixes.interchange.render.GradedArterialRasterizer;
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
    private final GradedArterialRasterizer arterialRasterizer =
            new GradedArterialRasterizer();
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
        ChunkRoadSurface unaffectedSurface = unaffectedNativeSurface(
                targetChunk, nativeRoads, geometry);
        ChunkRoadSurface interchangeSurface = interchangeRasterizer.rasterize(
                targetChunk, geometry);
        return finish(targetChunk, unaffectedSurface, interchangeSurface);
    }

    /**
     * Preserves both through highways while omitting an invalid ramp overlay.
     * This is the world-generation fallback; it must never emit a partial
     * interchange or revert to the unsafe native crossing elevations.
     */
    public ChunkRoadSurface composeStraightThrough(
            ChunkPoint targetChunk,
            Collection<ElevatedRoadTile> nativeRoads,
            Collection<PlannedInterchangeGeometry> interchanges) {
        Objects.requireNonNull(targetChunk, "targetChunk");
        Objects.requireNonNull(nativeRoads, "nativeRoads");
        Objects.requireNonNull(interchanges, "interchanges");
        List<PlannedInterchangeGeometry> geometry = List.copyOf(interchanges);
        ChunkRoadSurface unaffectedSurface = unaffectedNativeSurface(
                targetChunk, nativeRoads, geometry);
        List<GradedArterial> arterials = geometry.stream()
                .flatMap(interchange -> interchange.arterials().stream())
                .toList();
        ChunkRoadSurface arterialSurface = arterialRasterizer.rasterize(
                targetChunk, arterials);
        return finish(targetChunk, unaffectedSurface, arterialSurface);
    }

    private ChunkRoadSurface unaffectedNativeSurface(
            ChunkPoint targetChunk,
            Collection<ElevatedRoadTile> nativeRoads,
            List<PlannedInterchangeGeometry> geometry) {
        ChunkRoadSurface nativeSurface = roadRasterizer.rasterize(targetChunk, nativeRoads);
        return new ChunkRoadSurface(
                targetChunk,
                nativeSurface.cells().stream()
                        .filter(cell -> geometry.stream().noneMatch(interchange ->
                                interchange.replacesNativeCell(cell.position())))
                        .toList());
    }

    private ChunkRoadSurface finish(
            ChunkPoint targetChunk,
            ChunkRoadSurface unaffectedSurface,
            ChunkRoadSurface protectedSurface) {
        ChunkRoadSurface composed = protectedInterchangeMerger.merge(
                targetChunk, unaffectedSurface, protectedSurface);
        clearanceValidator.requireSafe(composed);
        return composed;
    }
}
