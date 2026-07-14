package net.austizz.lostcitiesroadfixes.render;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.Objects;

public final class MinecraftRoadPalette {
    public static final MinecraftRoadPalette DEFAULT = new MinecraftRoadPalette();

    private MinecraftRoadPalette() {
    }

    public BlockState fullBlock(RoadSurfaceRole role) {
        return switch (Objects.requireNonNull(role, "role")) {
            case ASPHALT, AT_GRADE_INTERSECTION -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            case SHOULDER, MEDIAN -> Blocks.SMOOTH_STONE.defaultBlockState();
            case WHITE_MARKING -> Blocks.QUARTZ_BLOCK.defaultBlockState();
            case YELLOW_MARKING -> Blocks.CUT_SANDSTONE.defaultBlockState();
        };
    }

    public BlockState bottomSlab(RoadSurfaceRole role) {
        BlockState state = switch (Objects.requireNonNull(role, "role")) {
            case ASPHALT, AT_GRADE_INTERSECTION -> Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState();
            case SHOULDER, MEDIAN -> Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
            case WHITE_MARKING -> Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState();
            case YELLOW_MARKING -> Blocks.CUT_SANDSTONE_SLAB.defaultBlockState();
        };
        return state.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    public BlockState foundation() {
        return Blocks.SMOOTH_STONE.defaultBlockState();
    }
}
