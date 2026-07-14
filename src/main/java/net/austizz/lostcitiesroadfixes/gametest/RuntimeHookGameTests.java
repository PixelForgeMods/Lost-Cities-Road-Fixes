package net.austizz.lostcitiesroadfixes.gametest;

import mcjty.lostcities.worldgen.gen.Highways;
import net.austizz.lostcitiesroadfixes.LostCitiesRoadFixes;
import net.austizz.lostcitiesroadfixes.config.RoadFixesServerConfig;
import net.austizz.lostcitiesroadfixes.config.RoadOperationalSettings;
import net.austizz.lostcitiesroadfixes.integration.RoadGenerationRuntime;
import net.austizz.lostcitiesroadfixes.integration.RuntimeRoadRenderPipeline;
import net.austizz.lostcitiesroadfixes.render.ChunkRoadSurface;
import net.austizz.lostcitiesroadfixes.render.MinecraftRoadWriter;
import net.austizz.lostcitiesroadfixes.render.MinecraftRoadPalette;
import net.austizz.lostcitiesroadfixes.render.RoadSupportPolicy;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceCell;
import net.austizz.lostcitiesroadfixes.render.RoadSurfacePosition;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRole;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.theme.CompiledRoadTheme;
import net.austizz.lostcitiesroadfixes.theme.RoadTheme;
import net.austizz.lostcitiesroadfixes.theme.RoadThemeCatalogue;
import net.austizz.lostcitiesroadfixes.theme.RoadThemeCompiler;
import net.austizz.lostcitiesroadfixes.theme.RoadThemeId;
import net.austizz.lostcitiesroadfixes.theme.RoadThemeResources;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
    public static void runtimePipelineWritesOneComposedSurface(GameTestHelper helper) {
        AtomicInteger writes = new AtomicInteger();

        new RuntimeRoadRenderPipeline().render(
                new ChunkPoint(0, 0),
                List.of(),
                List.of(),
                surface -> writes.incrementAndGet());

        if (writes.get() != 1) {
            helper.fail("Runtime road pipeline wrote " + writes.get() + " surfaces instead of one");
            return;
        }
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

    @GameTest(template = "empty3x3x3", timeoutTicks = 20)
    public static void minecraftWriterUsesThemeAndHonorsTheSupportPolicy(GameTestHelper helper) {
        BlockPos supported = new BlockPos(1, 2, 1);
        BlockPos unsupported = new BlockPos(2, 2, 1);
        BlockPos terrainStopped = new BlockPos(0, 2, 1);
        helper.setBlock(supported.below(), Blocks.AIR);
        helper.setBlock(supported.below(2), Blocks.WATER);
        helper.setBlock(unsupported.below(), Blocks.AIR);
        helper.setBlock(unsupported.below(2), Blocks.AIR);
        helper.setBlock(terrainStopped.below(), Blocks.AIR);
        helper.setBlock(terrainStopped.below(2), Blocks.STONE);

        MinecraftRoadPalette palette = testPalette();
        MinecraftRoadWriter writer = new MinecraftRoadWriter(palette);
        writeCell(helper, writer, supported, HalfBlockElevation.ofWholeBlocks(
                helper.absolutePos(supported).getY()), RoadSurfaceRole.ASPHALT,
                RoadSupportPolicy.enabled(1));
        writeCell(helper, writer, unsupported, HalfBlockElevation.ofWholeBlocks(
                helper.absolutePos(unsupported).getY()), RoadSurfaceRole.ASPHALT,
                RoadSupportPolicy.disabled());
        writeCell(helper, writer, terrainStopped, HalfBlockElevation.ofWholeBlocks(
                helper.absolutePos(terrainStopped).getY()), RoadSurfaceRole.ASPHALT,
                RoadSupportPolicy.enabled(2));

        helper.assertBlockPresent(Blocks.BLUE_CONCRETE, supported);
        helper.assertBlockPresent(Blocks.IRON_BLOCK, supported.below());
        helper.assertBlockPresent(Blocks.DEEPSLATE_BRICKS, supported.below(2));
        helper.assertBlockPresent(Blocks.BLUE_CONCRETE, unsupported);
        helper.assertBlockPresent(Blocks.IRON_BLOCK, unsupported.below());
        helper.assertBlockPresent(Blocks.AIR, unsupported.below(2));
        helper.assertBlockPresent(Blocks.IRON_BLOCK, terrainStopped.below());
        helper.assertBlockPresent(Blocks.STONE, terrainStopped.below(2));
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", timeoutTicks = 20)
    public static void roadThemeCompilerValidatesRegistryStatesAndSlabs(GameTestHelper helper) {
        RoadTheme base = RoadThemeCatalogue.defaultTheme();
        Map<RoadSurfaceRole, String> full = new EnumMap<>(base.fullBlocks());
        Map<RoadSurfaceRole, String> slabs = new EnumMap<>(base.bottomSlabs());
        full.put(RoadSurfaceRole.ASPHALT, "minecraft:blue_concrete");
        slabs.put(
                RoadSurfaceRole.ASPHALT,
                "minecraft:oxidized_cut_copper_slab[type=bottom,waterlogged=false]");
        RoadTheme valid = new RoadTheme(
                RoadThemeId.parse("example:valid"),
                full,
                slabs,
                "minecraft:iron_block",
                "minecraft:deepslate_bricks",
                40);

        CompiledRoadTheme compiled = new RoadThemeCompiler().compile(valid);
        if (!compiled.palette().fullBlock(RoadSurfaceRole.ASPHALT)
                .is(Blocks.BLUE_CONCRETE)) {
            helper.fail("Custom full-block theme state was not compiled");
            return;
        }
        if (compiled.palette().bottomSlab(RoadSurfaceRole.ASPHALT)
                .getValue(SlabBlock.TYPE) != SlabType.BOTTOM) {
            helper.fail("Custom slab properties were not compiled");
            return;
        }

        Map<RoadSurfaceRole, String> unknownBlocks = new EnumMap<>(full);
        unknownBlocks.put(RoadSurfaceRole.ASPHALT, "missing:not_a_block");
        assertThemeRejected(helper, new RoadTheme(
                RoadThemeId.parse("example:unknown"),
                unknownBlocks,
                slabs,
                valid.foundation(),
                valid.support(),
                40), "missing:not_a_block");

        Map<RoadSurfaceRole, String> topSlabs = new EnumMap<>(slabs);
        topSlabs.put(
                RoadSurfaceRole.ASPHALT,
                "minecraft:oxidized_cut_copper_slab[type=top,waterlogged=false]");
        assertThemeRejected(helper, new RoadTheme(
                RoadThemeId.parse("example:top_slab"),
                full,
                topSlabs,
                valid.foundation(),
                valid.support(),
                40), "bottom slab");

        assertThemeRejected(helper, new RoadTheme(
                RoadThemeId.parse("example:air_support"),
                full,
                slabs,
                valid.foundation(),
                "minecraft:air",
                40), "solid block");

        CompiledRoadTheme before = RoadThemeResources.active();
        Map<RoadSurfaceRole, String> invalidDefaultBlocks = new EnumMap<>(full);
        invalidDefaultBlocks.put(RoadSurfaceRole.ASPHALT, "missing:not_a_block");
        RoadTheme invalidDefault = new RoadTheme(
                RoadThemeCatalogue.DEFAULT_ID,
                invalidDefaultBlocks,
                slabs,
                valid.foundation(),
                valid.support(),
                40);
        try {
            RoadThemeResources.install(Map.of(RoadThemeCatalogue.DEFAULT_ID, invalidDefault));
            helper.fail("Invalid theme reload should have failed atomically");
            return;
        } catch (IllegalArgumentException expected) {
            if (RoadThemeResources.active() != before) {
                helper.fail("Invalid theme reload replaced the active compiled snapshot");
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", timeoutTicks = 20)
    public static void operatorConfigAndCommandsAreLiveOnTheServer(GameTestHelper helper)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        RoadOperationalSettings settings = RoadFixesServerConfig.settings();
        if (settings.maximumGapChunks() < RoadOperationalSettings.MINIMUM_GAP_CHUNKS
                || settings.maximumGapChunks() > RoadOperationalSettings.MAXIMUM_GAP_CHUNKS
                || settings.maximumCachedRegions()
                        < RoadOperationalSettings.MINIMUM_CACHED_REGIONS
                || settings.maximumCachedRegions()
                        > RoadOperationalSettings.MAXIMUM_CACHED_REGIONS) {
            helper.fail("Loaded road-fix server config escaped its safety bounds");
            return;
        }
        if (!RoadThemeResources.active(RoadThemeId.parse("missing:not_loaded"))
                .id().equals(RoadThemeCatalogue.DEFAULT_ID)) {
            helper.fail("Unavailable configured themes must resolve to the built-in default");
            return;
        }

        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        var root = dispatcher.getRoot().getChild(LostCitiesRoadFixes.MOD_ID);
        if (root == null
                || root.getChild("status") == null
                || root.getChild("clear_caches") == null
                || dispatcher.getRoot().getChild("lcroadfixes") == null) {
            helper.fail("Road-fix operator commands were not registered");
            return;
        }
        int statusLines = dispatcher.execute(
                LostCitiesRoadFixes.MOD_ID + " status",
                server.createCommandSourceStack());
        if (statusLines != 8) {
            helper.fail("Status command returned " + statusLines + " lines instead of 8");
            return;
        }
        dispatcher.execute(
                LostCitiesRoadFixes.MOD_ID + " clear_caches",
                server.createCommandSourceStack());
        if (RoadGenerationRuntime.roadPlanCacheSize() != 0
                || RoadGenerationRuntime.interchangePlanCacheSize() != 0) {
            helper.fail("Cache clear command did not replace both cache generations");
            return;
        }
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

    private static void writeCell(
            GameTestHelper helper,
            MinecraftRoadWriter writer,
            BlockPos relative,
            HalfBlockElevation elevation,
            RoadSurfaceRole role,
            RoadSupportPolicy supportPolicy) {
        BlockPos absolute = helper.absolutePos(relative);
        ChunkPoint chunkPoint = new ChunkPoint(absolute.getX() >> 4, absolute.getZ() >> 4);
        ChunkRoadSurface surface = new ChunkRoadSurface(chunkPoint, List.of(new RoadSurfaceCell(
                new RoadSurfacePosition(absolute.getX(), absolute.getZ(), elevation), role)));
        writer.write(helper.getLevel().getChunkAt(absolute), surface, supportPolicy);
    }

    private static MinecraftRoadPalette testPalette() {
        Map<RoadSurfaceRole, net.minecraft.world.level.block.state.BlockState> full =
                new EnumMap<>(RoadSurfaceRole.class);
        Map<RoadSurfaceRole, net.minecraft.world.level.block.state.BlockState> slabs =
                new EnumMap<>(RoadSurfaceRole.class);
        for (RoadSurfaceRole role : RoadSurfaceRole.values()) {
            full.put(role, Blocks.BLUE_CONCRETE.defaultBlockState());
            slabs.put(role, Blocks.SMOOTH_STONE_SLAB.defaultBlockState()
                    .setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        }
        return new MinecraftRoadPalette(
                full,
                slabs,
                Blocks.IRON_BLOCK.defaultBlockState(),
                Blocks.DEEPSLATE_BRICKS.defaultBlockState());
    }

    private static void assertThemeRejected(
            GameTestHelper helper,
            RoadTheme theme,
            String expectedMessage) {
        try {
            new RoadThemeCompiler().compile(theme);
            helper.fail("Road theme " + theme.id() + " should have been rejected");
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(theme.id().toString())
                    || !exception.getMessage().contains(expectedMessage)) {
                helper.fail("Unexpected road-theme diagnostic: " + exception.getMessage());
            }
        }
    }
}
