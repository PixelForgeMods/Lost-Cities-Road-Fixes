package net.austizz.lostcitiesroadfixes.interchange.render;

import net.austizz.lostcitiesroadfixes.interchange.geometry.RampRoute;
import net.austizz.lostcitiesroadfixes.render.ChunkRoadSurface;
import net.austizz.lostcitiesroadfixes.render.RampSurfaceRasterizer;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class InterchangeSurfaceRasterizer {
    private final GradedArterialRasterizer arterialRasterizer = new GradedArterialRasterizer();
    private final RampSurfaceRasterizer rampRasterizer = new RampSurfaceRasterizer();
    private final ChunkRoadSurfaceMerger merger = new ChunkRoadSurfaceMerger();

    public ChunkRoadSurface rasterize(
            ChunkPoint targetChunk,
            Collection<PlannedInterchangeGeometry> interchanges) {
        Objects.requireNonNull(targetChunk, "targetChunk");
        Objects.requireNonNull(interchanges, "interchanges");
        List<GradedArterial> arterials = new ArrayList<>();
        List<RampRoute> ramps = new ArrayList<>();
        for (PlannedInterchangeGeometry interchange : interchanges) {
            arterials.addAll(interchange.arterials());
            ramps.addAll(interchange.turningRoutes());
        }
        return merger.merge(targetChunk, List.of(
                arterialRasterizer.rasterize(targetChunk, arterials),
                rampRasterizer.rasterize(targetChunk, ramps)));
    }
}
