package net.austizz.lostcitiesroadfixes.integration;

import net.austizz.lostcitiesroadfixes.interchange.render.PlannedInterchangeGeometry;
import net.austizz.lostcitiesroadfixes.render.ChunkRoadSurface;
import net.austizz.lostcitiesroadfixes.render.ElevatedRoadTile;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

public final class RuntimeRoadRenderPipeline {
    private final RuntimeRoadSurfaceComposer composer;

    public RuntimeRoadRenderPipeline() {
        this(new RuntimeRoadSurfaceComposer());
    }

    RuntimeRoadRenderPipeline(RuntimeRoadSurfaceComposer composer) {
        this.composer = Objects.requireNonNull(composer, "composer");
    }

    public void render(
            ChunkPoint targetChunk,
            Collection<ElevatedRoadTile> nativeRoads,
            Collection<PlannedInterchangeGeometry> interchanges,
            Consumer<ChunkRoadSurface> writer) {
        Objects.requireNonNull(writer, "writer").accept(
                compose(targetChunk, nativeRoads, interchanges));
    }

    public ChunkRoadSurface compose(
            ChunkPoint targetChunk,
            Collection<ElevatedRoadTile> nativeRoads,
            Collection<PlannedInterchangeGeometry> interchanges) {
        return composer.compose(targetChunk, nativeRoads, interchanges);
    }
}
