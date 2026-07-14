package net.austizz.lostcitiesroadfixes.integration;

import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.worldgen.LostCityTerrainFeature;
import net.austizz.lostcitiesroadfixes.LostCitiesRoadFixes;
import net.austizz.lostcitiesroadfixes.planning.RegionalPlanCache;
import net.austizz.lostcitiesroadfixes.planning.RoadPlanKey;
import net.austizz.lostcitiesroadfixes.planning.continuity.ContinuityPlanner;
import net.austizz.lostcitiesroadfixes.planning.continuity.RegionalRoadPlan;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
import net.austizz.lostcitiesroadfixes.render.ElevatedRoadTile;
import net.austizz.lostcitiesroadfixes.render.MinecraftRoadWriter;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRasterizer;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.PlanningGrid;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class RoadGenerationRuntime {
    private static final ContinuityPlanner CONTINUITY_PLANNER = new ContinuityPlanner(4);
    private static final RegionalPlanCache<RegionalRoadPlan> PLAN_CACHE = new RegionalPlanCache<>();
    private static final RoadSurfaceRasterizer RASTERIZER = new RoadSurfaceRasterizer();
    private static final MinecraftRoadWriter WRITER = new MinecraftRoadWriter();
    private static final AtomicLong NATIVE_SUPPRESSIONS = new AtomicLong();
    private static final AtomicLong LATE_RENDER_INVOCATIONS = new AtomicLong();

    private RoadGenerationRuntime() {
    }

    public static void suppressNativeHighway() {
        if (NATIVE_SUPPRESSIONS.incrementAndGet() == 1) {
            LostCitiesRoadFixes.LOGGER.info("Native Lost Cities highway placement is suppressed");
        }
    }

    public static long nativeSuppressionCount() {
        return NATIVE_SUPPRESSIONS.get();
    }

    public static long lateRenderInvocationCount() {
        return LATE_RENDER_INVOCATIONS.get();
    }

    public static void renderAfterLostCities(
            LostCityTerrainFeature feature,
            WorldGenRegion region,
            ChunkAccess chunk) {
        Objects.requireNonNull(feature, "feature");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(chunk, "chunk");
        ChunkPoint target = new ChunkPoint(chunk.getPos().x, chunk.getPos().z);

        try {
            if (LATE_RENDER_INVOCATIONS.incrementAndGet() == 1) {
                LostCitiesRoadFixes.LOGGER.info(
                        "Post-cleanup replacement road rendering is active in {}",
                        feature.provider.getType().location());
            }
            List<ElevatedRoadTile> roads = nearbyRoads(feature, target);
            WRITER.write(chunk, RASTERIZER.rasterize(target, roads));
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to render replacement roads in dimension "
                            + feature.provider.getType().location() + " at chunk " + target,
                    exception);
        }
    }

    public static void invalidatePlans() {
        PLAN_CACHE.invalidateAll();
    }

    private static List<ElevatedRoadTile> nearbyRoads(
            LostCityTerrainFeature feature,
            ChunkPoint target) {
        List<ElevatedRoadTile> roads = new ArrayList<>();
        for (int offset = -1; offset <= 1; offset++) {
            addIfPresent(feature, new ChunkPoint(target.x(), target.z() + offset), RoadAxis.X, roads);
            addIfPresent(feature, new ChunkPoint(target.x() + offset, target.z()), RoadAxis.Z, roads);
        }
        return List.copyOf(roads);
    }

    private static void addIfPresent(
            LostCityTerrainFeature feature,
            ChunkPoint candidate,
            RoadAxis axis,
            List<ElevatedRoadTile> roads) {
        RegionalRoadPlan plan = planFor(feature, candidate);
        plan.tileAt(candidate, axis).ifPresent(tile -> {
            int blockY = Math.addExact(
                    feature.profile.GROUNDLEVEL,
                    Math.multiplyExact(tile.level(), LostCityTerrainFeature.FLOORHEIGHT));
            roads.add(new ElevatedRoadTile(
                    candidate,
                    axis,
                    HalfBlockElevation.ofWholeBlocks(blockY)));
        });
    }

    private static RegionalRoadPlan planFor(LostCityTerrainFeature feature, ChunkPoint chunk) {
        RoadPlanKey key = new RoadPlanKey(
                feature.provider.getSeed(),
                feature.provider.getType().location().toString(),
                PlanningGrid.regionFor(chunk),
                rulesFingerprint(feature.profile));
        return PLAN_CACHE.getOrPlan(key, ignored -> CONTINUITY_PLANNER.plan(
                key,
                new LostCitiesRoadObservationSource(feature.provider, feature.profile)));
    }

    private static String rulesFingerprint(LostCityProfile profile) {
        return "runtime-v1|" + profile.getName()
                + '|' + profile.GROUNDLEVEL
                + '|' + profile.HIGHWAY_DISTANCE_MASK
                + '|' + Float.toHexString(profile.HIGHWAY_MAINPERLIN_SCALE)
                + '|' + Float.toHexString(profile.HIGHWAY_SECONDARYPERLIN_SCALE)
                + '|' + Float.toHexString(profile.HIGHWAY_PERLIN_FACTOR)
                + '|' + profile.HIGHWAY_REQUIRES_TWO_CITIES
                + '|' + profile.HIGHWAY_LEVEL_FROM_CITIES_MODE;
    }
}
