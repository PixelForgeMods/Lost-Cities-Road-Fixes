package net.austizz.lostcitiesroadfixes.gametest;

import mcjty.lostcities.worldgen.gen.Highways;
import net.austizz.lostcitiesroadfixes.LostCitiesRoadFixes;
import net.austizz.lostcitiesroadfixes.integration.RoadGenerationRuntime;
import net.austizz.lostcitiesroadfixes.render.ChunkRoadSurface;
import net.austizz.lostcitiesroadfixes.render.MinecraftRoadWriter;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceCell;
import net.austizz.lostcitiesroadfixes.render.RoadSurfacePosition;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRole;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(LostCitiesRoadFixes.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RuntimeHookGameTests {
    private RuntimeHookGameTests() {
    }

    @GameTest(template = "empty3x3x3", timeoutTicks = 20)
    public static void nativeHighwayPlacementIsActuallyCancelled(GameTestHelper helper) {
        long before = RoadGenerationRuntime.nativeSuppressionCount();

        Highways.generateHighways(null, null);

        if (RoadGenerationRuntime.nativeSuppressionCount() != before + 1) {
            helper.fail("The transformed Highways.generateHighways call was not cancelled");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", timeoutTicks = 20)
    public static void lateTerrainHookTargetTransformsSuccessfully(GameTestHelper helper)
            throws ClassNotFoundException {
        Class.forName("mcjty.lostcities.worldgen.LostCityTerrainFeature", true,
                RuntimeHookGameTests.class.getClassLoader());
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", timeoutTicks = 20)
    public static void minecraftWriterPlacesFullAndHalfSurfacesAfterClearingHeadroom(GameTestHelper helper) {
        BlockPos fullRelative = new BlockPos(1, 1, 1);
        BlockPos halfRelative = new BlockPos(2, 1, 1);
        helper.setBlock(fullRelative.above(), Blocks.STONE);
        helper.setBlock(halfRelative.above(), Blocks.STONE);

        MinecraftRoadWriter writer = new MinecraftRoadWriter();
        writeCell(helper, writer, fullRelative, HalfBlockElevation.ofWholeBlocks(
                helper.absolutePos(fullRelative).getY()), RoadSurfaceRole.ASPHALT);
        writeCell(helper, writer, halfRelative, new HalfBlockElevation(
                helper.absolutePos(halfRelative).getY() * 2 + 1), RoadSurfaceRole.WHITE_MARKING);

        helper.assertBlockPresent(Blocks.POLISHED_DEEPSLATE, fullRelative);
        helper.assertBlockPresent(Blocks.AIR, fullRelative.above());
        helper.assertBlockPresent(Blocks.SMOOTH_QUARTZ_SLAB, halfRelative);
        helper.assertBlockProperty(halfRelative, SlabBlock.TYPE, SlabType.BOTTOM);
        helper.assertBlockPresent(Blocks.AIR, halfRelative.above());
        helper.succeed();
    }

    private static void writeCell(
            GameTestHelper helper,
            MinecraftRoadWriter writer,
            BlockPos relative,
            HalfBlockElevation elevation,
            RoadSurfaceRole role) {
        BlockPos absolute = helper.absolutePos(relative);
        ChunkPoint chunkPoint = new ChunkPoint(absolute.getX() >> 4, absolute.getZ() >> 4);
        ChunkRoadSurface surface = new ChunkRoadSurface(chunkPoint, List.of(new RoadSurfaceCell(
                new RoadSurfacePosition(absolute.getX(), absolute.getZ(), elevation), role)));
        writer.write(helper.getLevel().getChunkAt(absolute), surface);
    }
}
