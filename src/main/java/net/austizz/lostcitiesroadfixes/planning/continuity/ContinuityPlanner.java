package net.austizz.lostcitiesroadfixes.planning.continuity;

import net.austizz.lostcitiesroadfixes.planning.RoadPlanKey;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

public final class ContinuityPlanner {
    private final int maximumGapChunks;

    public ContinuityPlanner(int maximumGapChunks) {
        if (maximumGapChunks < 1) {
            throw new IllegalArgumentException("Maximum continuity gap must be at least one chunk");
        }
        this.maximumGapChunks = maximumGapChunks;
    }

    public RegionalRoadPlan plan(RoadPlanKey key, RoadObservationSource source) {
        return plan(key, ChunkBounds.planningWindow(key.region()), source);
    }

    public RegionalRoadPlan plan(RoadPlanKey key, ChunkBounds bounds, RoadObservationSource source) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(source, "source");

        Map<RoadTileKey, RoadTile> snapshot = sample(bounds, source);
        Map<RoadTileKey, RoadTile> completed = new HashMap<>(snapshot);
        repairXGaps(bounds, snapshot, completed);
        repairZGaps(bounds, snapshot, completed);

        List<RoadTile> ownedTiles = completed.values().stream()
                .filter(tile -> key.region().owns(tile.key().chunk()))
                .toList();
        return new RegionalRoadPlan(key, ownedTiles);
    }

    private static Map<RoadTileKey, RoadTile> sample(
            ChunkBounds bounds,
            RoadObservationSource source) {
        Map<RoadTileKey, RoadTile> snapshot = new HashMap<>();
        for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                ChunkPoint chunk = new ChunkPoint(x, z);
                for (RoadAxis axis : RoadAxis.values()) {
                    OptionalInt level = source.roadLevel(chunk, axis);
                    if (level.isPresent()) {
                        RoadTile tile = new RoadTile(
                                new RoadTileKey(chunk, axis),
                                level.getAsInt(),
                                RoadTileOrigin.OBSERVED);
                        snapshot.put(tile.key(), tile);
                    }
                }
            }
        }
        return snapshot;
    }

    private void repairXGaps(
            ChunkBounds bounds,
            Map<RoadTileKey, RoadTile> snapshot,
            Map<RoadTileKey, RoadTile> completed) {
        for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
            RoadTile previous = null;
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                RoadTile current = snapshot.get(new RoadTileKey(new ChunkPoint(x, z), RoadAxis.X));
                if (current != null) {
                    repairBetween(previous, current, completed);
                    previous = current;
                }
            }
        }
    }

    private void repairZGaps(
            ChunkBounds bounds,
            Map<RoadTileKey, RoadTile> snapshot,
            Map<RoadTileKey, RoadTile> completed) {
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            RoadTile previous = null;
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                RoadTile current = snapshot.get(new RoadTileKey(new ChunkPoint(x, z), RoadAxis.Z));
                if (current != null) {
                    repairBetween(previous, current, completed);
                    previous = current;
                }
            }
        }
    }

    private void repairBetween(
            RoadTile start,
            RoadTile end,
            Map<RoadTileKey, RoadTile> completed) {
        if (start == null || start.key().axis() != end.key().axis() || start.level() != end.level()) {
            return;
        }

        int distance = start.key().axis() == RoadAxis.X
                ? end.key().chunk().x() - start.key().chunk().x()
                : end.key().chunk().z() - start.key().chunk().z();
        int gap = distance - 1;
        if (gap < 1 || gap > maximumGapChunks) {
            return;
        }

        List<RoadTile> repairs = new ArrayList<>(gap);
        for (int offset = 1; offset <= gap; offset++) {
            ChunkPoint chunk = start.key().axis() == RoadAxis.X
                    ? new ChunkPoint(start.key().chunk().x() + offset, start.key().chunk().z())
                    : new ChunkPoint(start.key().chunk().x(), start.key().chunk().z() + offset);
            repairs.add(new RoadTile(
                    new RoadTileKey(chunk, start.key().axis()),
                    start.level(),
                    RoadTileOrigin.CONTINUITY_REPAIR));
        }
        repairs.forEach(tile -> completed.putIfAbsent(tile.key(), tile));
    }
}
