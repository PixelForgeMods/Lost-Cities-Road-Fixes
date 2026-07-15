package net.austizz.lostcitiesroadfixes.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MinecraftRoadWriter {
    private final MinecraftRoadPalette palette;
    private final RoadSupportPlanner supportPlanner = new RoadSupportPlanner();
    private final RoadClearanceEnvelopePlanner clearancePlanner =
            new RoadClearanceEnvelopePlanner();

    public MinecraftRoadWriter() {
        this(MinecraftRoadPalette.DEFAULT);
    }

    public MinecraftRoadWriter(MinecraftRoadPalette palette) {
        this.palette = Objects.requireNonNull(palette, "palette");
    }

    public int write(ChunkAccess chunk, ChunkRoadSurface surface) {
        return write(chunk, surface, RoadSupportPolicy.disabled());
    }

    public int write(
            ChunkAccess chunk,
            ChunkRoadSurface surface,
            RoadSupportPolicy supportPolicy) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(supportPolicy, "supportPolicy");
        if (chunk.getPos().x != surface.chunk().x() || chunk.getPos().z != surface.chunk().z()) {
            throw new IllegalArgumentException("Chunk does not own the supplied road surface");
        }

        int writes = 0;
        Map<RoadSurfacePosition, Integer> plannedHeadroom = clearancePlanner.plan(surface);
        Map<Column, List<Integer>> deckElevations = deckElevations(surface);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (RoadSurfaceCell cell : surface.cells()) {
            int y = cell.position().elevation().floorBlockY();
            if (y <= chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) {
                continue;
            }

            cursor.set(cell.position().x(), y - 1, cell.position().z());
            chunk.setBlockState(cursor, palette.foundation(), false);

            cursor.setY(y);
            boolean halfBlock = Math.floorMod(cell.position().elevation().halfBlocks(), 2) != 0;
            chunk.setBlockState(cursor,
                    halfBlock ? palette.bottomSlab(cell.role()) : palette.fullBlock(cell.role()),
                    false);

            int clearTop = Math.min(
                    y + plannedHeadroom.get(cell.position()),
                    chunk.getMaxBuildHeight() - 1);
            clearTop = capBelowNextDeck(
                    clearTop,
                    y,
                    deckElevations.get(new Column(
                            cell.position().x(), cell.position().z())));
            for (int clearY = y + 1; clearY <= clearTop; clearY++) {
                cursor.setY(clearY);
                var existing = chunk.getBlockState(cursor);
                if (existing.getDestroySpeed(chunk, cursor) >= 0.0f) {
                    chunk.setBlockState(cursor, Blocks.AIR.defaultBlockState(), false);
                }
            }
            writes++;
        }
        if (supportPolicy.enabled()) {
            for (RoadSurfacePosition anchor : supportPlanner.anchors(surface)) {
                writeSupport(chunk, cursor, anchor, supportPolicy.maximumDepthBlocks());
            }
        }
        return writes;
    }

    private static Map<Column, List<Integer>> deckElevations(ChunkRoadSurface surface) {
        Map<Column, List<Integer>> result = new HashMap<>();
        for (RoadSurfaceCell cell : surface.cells()) {
            Column column = new Column(cell.position().x(), cell.position().z());
            result.computeIfAbsent(column, ignored -> new ArrayList<>())
                    .add(cell.position().elevation().floorBlockY());
        }
        result.values().forEach(elevations -> elevations.sort(Integer::compareTo));
        return result;
    }

    private static int capBelowNextDeck(
            int requestedTop,
            int currentDeckY,
            List<Integer> deckElevations) {
        if (deckElevations == null) {
            return requestedTop;
        }
        for (int deckY : deckElevations) {
            if (deckY > currentDeckY) {
                return StrictMath.min(requestedTop, deckY - 2);
            }
        }
        return requestedTop;
    }

    private void writeSupport(
            ChunkAccess chunk,
            BlockPos.MutableBlockPos cursor,
            RoadSurfacePosition anchor,
            int maximumDepthBlocks) {
        int surfaceY = anchor.elevation().floorBlockY();
        if (surfaceY <= chunk.getMinBuildHeight()
                || surfaceY >= chunk.getMaxBuildHeight()) {
            return;
        }
        int y = surfaceY - 2;
        for (int depth = 0;
                depth < maximumDepthBlocks && y >= chunk.getMinBuildHeight();
                depth++, y--) {
            cursor.set(anchor.x(), y, anchor.z());
            var existing = chunk.getBlockState(cursor);
            boolean liquidWithoutCollision = !existing.getFluidState().isEmpty()
                    && existing.getCollisionShape(chunk, cursor).isEmpty();
            if (!existing.isAir() && !liquidWithoutCollision) {
                break;
            }
            chunk.setBlockState(cursor, palette.support(), false);
        }
    }

    private record Column(int x, int z) {
    }
}
