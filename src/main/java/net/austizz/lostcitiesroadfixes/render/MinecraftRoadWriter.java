package net.austizz.lostcitiesroadfixes.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Objects;

public final class MinecraftRoadWriter {
    public static final int HEADROOM_BLOCKS = 7;

    private final MinecraftRoadPalette palette;

    public MinecraftRoadWriter() {
        this(MinecraftRoadPalette.DEFAULT);
    }

    public MinecraftRoadWriter(MinecraftRoadPalette palette) {
        this.palette = Objects.requireNonNull(palette, "palette");
    }

    public int write(ChunkAccess chunk, ChunkRoadSurface surface) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(surface, "surface");
        if (chunk.getPos().x != surface.chunk().x() || chunk.getPos().z != surface.chunk().z()) {
            throw new IllegalArgumentException("Chunk does not own the supplied road surface");
        }

        int writes = 0;
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

            int clearTop = Math.min(y + HEADROOM_BLOCKS, chunk.getMaxBuildHeight() - 1);
            for (int clearY = y + 1; clearY <= clearTop; clearY++) {
                cursor.setY(clearY);
                var existing = chunk.getBlockState(cursor);
                if (existing.getDestroySpeed(chunk, cursor) >= 0.0f) {
                    chunk.setBlockState(cursor, Blocks.AIR.defaultBlockState(), false);
                }
            }
            writes++;
        }
        return writes;
    }
}
