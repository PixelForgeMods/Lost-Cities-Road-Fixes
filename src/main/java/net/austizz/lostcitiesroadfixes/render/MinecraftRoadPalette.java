package net.austizz.lostcitiesroadfixes.render;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class MinecraftRoadPalette {
    public static final MinecraftRoadPalette DEFAULT = createDefault();

    private final Map<RoadSurfaceRole, BlockState> fullBlocks;
    private final Map<RoadSurfaceRole, BlockState> bottomSlabs;
    private final BlockState foundation;
    private final BlockState support;

    public MinecraftRoadPalette(
            Map<RoadSurfaceRole, BlockState> fullBlocks,
            Map<RoadSurfaceRole, BlockState> bottomSlabs,
            BlockState foundation,
            BlockState support) {
        this.fullBlocks = completeRoleMap(fullBlocks, "full block");
        this.bottomSlabs = completeRoleMap(bottomSlabs, "bottom slab");
        for (Map.Entry<RoadSurfaceRole, BlockState> entry : this.fullBlocks.entrySet()) {
            BlockState state = entry.getValue();
            if (state.getBlock() instanceof SlabBlock
                    || state.isAir()
                    || !state.getFluidState().isEmpty()
                    || state.hasBlockEntity()) {
                throw new IllegalArgumentException(
                        "Road role " + entry.getKey()
                                + " must use a solid full block without a block entity");
            }
        }
        for (Map.Entry<RoadSurfaceRole, BlockState> entry : this.bottomSlabs.entrySet()) {
            if (!(entry.getValue().getBlock() instanceof SlabBlock)
                    || entry.getValue().getValue(SlabBlock.TYPE) != SlabType.BOTTOM
                    || !entry.getValue().getFluidState().isEmpty()
                    || entry.getValue().hasBlockEntity()) {
                throw new IllegalArgumentException(
                        "Road role " + entry.getKey()
                                + " must use a dry bottom slab state without a block entity");
            }
        }
        this.foundation = requireStructural(foundation, "foundation");
        this.support = requireStructural(support, "support");
    }

    public BlockState fullBlock(RoadSurfaceRole role) {
        return fullBlocks.get(Objects.requireNonNull(role, "role"));
    }

    public BlockState bottomSlab(RoadSurfaceRole role) {
        return bottomSlabs.get(Objects.requireNonNull(role, "role"));
    }

    public BlockState foundation() {
        return foundation;
    }

    public BlockState support() {
        return support;
    }

    private static Map<RoadSurfaceRole, BlockState> completeRoleMap(
            Map<RoadSurfaceRole, BlockState> source,
            String description) {
        Objects.requireNonNull(source, description + " map");
        EnumMap<RoadSurfaceRole, BlockState> result = new EnumMap<>(RoadSurfaceRole.class);
        for (RoadSurfaceRole role : RoadSurfaceRole.values()) {
            BlockState state = source.get(role);
            if (state == null) {
                throw new IllegalArgumentException(
                        "Missing " + description + " for road role " + role);
            }
            result.put(role, state);
        }
        if (source.size() != result.size()) {
            throw new IllegalArgumentException(description + " map contains unknown road roles");
        }
        return Map.copyOf(result);
    }

    private static BlockState requireStructural(BlockState state, String description) {
        Objects.requireNonNull(state, description);
        if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()) {
            throw new IllegalArgumentException(
                    "Road " + description + " must be a solid block without a block entity");
        }
        return state;
    }

    private static MinecraftRoadPalette createDefault() {
        EnumMap<RoadSurfaceRole, BlockState> full = new EnumMap<>(RoadSurfaceRole.class);
        EnumMap<RoadSurfaceRole, BlockState> slabs = new EnumMap<>(RoadSurfaceRole.class);
        for (RoadSurfaceRole role : RoadSurfaceRole.values()) {
            BlockState fullState = switch (role) {
                case ASPHALT, AT_GRADE_INTERSECTION ->
                        Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                case SHOULDER, MEDIAN -> Blocks.SMOOTH_STONE.defaultBlockState();
                case WHITE_MARKING -> Blocks.QUARTZ_BLOCK.defaultBlockState();
                case YELLOW_MARKING -> Blocks.CUT_SANDSTONE.defaultBlockState();
            };
            BlockState slabState = switch (role) {
                case ASPHALT, AT_GRADE_INTERSECTION ->
                        Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState();
                case SHOULDER, MEDIAN -> Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
                case WHITE_MARKING -> Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState();
                case YELLOW_MARKING -> Blocks.CUT_SANDSTONE_SLAB.defaultBlockState();
            };
            full.put(role, fullState);
            slabs.put(role, slabState.setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        }
        return new MinecraftRoadPalette(
                full,
                slabs,
                Blocks.SMOOTH_STONE.defaultBlockState(),
                Blocks.SMOOTH_STONE.defaultBlockState());
    }
}
