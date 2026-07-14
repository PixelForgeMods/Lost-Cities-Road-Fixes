package net.austizz.lostcitiesroadfixes.integration;

import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.worldgen.LostCityTerrainFeature;
import net.austizz.lostcitiesroadfixes.LostCitiesRoadFixes;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesign;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesignFingerprint;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesignResources;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeSelector;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeLayoutFactory;
import net.austizz.lostcitiesroadfixes.interchange.planning.CrossingElevationModel;
import net.austizz.lostcitiesroadfixes.interchange.planning.InterchangeRegionalPlanner;
import net.austizz.lostcitiesroadfixes.interchange.planning.RegionalInterchangePlan;
import net.austizz.lostcitiesroadfixes.interchange.planning.RoadCrossingSurveyor;
import net.austizz.lostcitiesroadfixes.interchange.planning.RoadTileLookup;
import net.austizz.lostcitiesroadfixes.interchange.render.InterchangeGeometryPlanner;
import net.austizz.lostcitiesroadfixes.interchange.render.PlannedInterchangeGeometry;
import net.austizz.lostcitiesroadfixes.planning.RoadPlanKey;
import net.austizz.lostcitiesroadfixes.planning.continuity.ContinuityPlanner;
import net.austizz.lostcitiesroadfixes.planning.continuity.RegionalRoadPlan;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
import net.austizz.lostcitiesroadfixes.render.ElevatedRoadTile;
import net.austizz.lostcitiesroadfixes.render.MinecraftRoadWriter;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.PlanningGrid;
import net.austizz.lostcitiesroadfixes.road.PlanningRegion;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class RoadGenerationRuntime {
    private static final int MAXIMUM_INTERCHANGE_APPROACH_BLOCKS = 256;
    private static final RoadDesignStandard ROAD_STANDARD = RoadDesignStandard.DEFAULT;
    private static final ContinuityPlanner CONTINUITY_PLANNER = new ContinuityPlanner(4);
    private static final RuntimePlanCaches<RegionalRoadPlan, RegionalInterchangeGeometryPlan>
            PLAN_CACHES = new RuntimePlanCaches<>();
    private static final InterchangeGeometryPlanner GEOMETRY_PLANNER =
            new InterchangeGeometryPlanner(new InterchangeLayoutFactory(ROAD_STANDARD));
    private static final RuntimeRoadRenderPipeline RENDER_PIPELINE =
            new RuntimeRoadRenderPipeline();
    private static final MinecraftRoadWriter WRITER = new MinecraftRoadWriter();
    private static final AtomicLong NATIVE_SUPPRESSIONS = new AtomicLong();
    private static final AtomicLong LATE_RENDER_INVOCATIONS = new AtomicLong();
    private static final AtomicLong INTERCHANGE_REGIONS_PLANNED = new AtomicLong();
    private static final AtomicLong SELECTED_INTERCHANGES_PLANNED = new AtomicLong();
    private static final AtomicLong REJECTED_CROSSINGS_PLANNED = new AtomicLong();
    private static final AtomicLong INTERCHANGE_RENDER_INVOCATIONS = new AtomicLong();

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

    public static long interchangeRegionPlanCount() {
        return INTERCHANGE_REGIONS_PLANNED.get();
    }

    public static long selectedInterchangePlanCount() {
        return SELECTED_INTERCHANGES_PLANNED.get();
    }

    public static long rejectedCrossingPlanCount() {
        return REJECTED_CROSSINGS_PLANNED.get();
    }

    public static long interchangeRenderInvocationCount() {
        return INTERCHANGE_RENDER_INVOCATIONS.get();
    }

    public static int roadPlanCacheSize() {
        return PLAN_CACHES.roadSize();
    }

    public static int interchangePlanCacheSize() {
        return PLAN_CACHES.interchangeSize();
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
            List<PlannedInterchangeGeometry> interchanges = nearbyInterchanges(feature, target);
            if (!interchanges.isEmpty()) {
                INTERCHANGE_RENDER_INVOCATIONS.incrementAndGet();
            }
            RENDER_PIPELINE.render(
                    target,
                    roads,
                    interchanges,
                    surface -> WRITER.write(chunk, surface));
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to render replacement roads in dimension "
                            + feature.provider.getType().location() + " at chunk " + target,
                    exception);
        }
    }

    public static void invalidatePlans() {
        PLAN_CACHES.invalidateAll();
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
        RoadPlanKey key = keyFor(
                feature,
                PlanningGrid.regionFor(chunk),
                roadRulesFingerprint(feature.profile));
        return PLAN_CACHES.roadPlan(key, ignored -> CONTINUITY_PLANNER.plan(
                key,
                new LostCitiesRoadObservationSource(feature.provider, feature.profile)));
    }

    private static List<PlannedInterchangeGeometry> nearbyInterchanges(
            LostCityTerrainFeature feature,
            ChunkPoint target) {
        List<InterchangeDesign> designs = InterchangeDesignResources.repository().snapshot();
        String designFingerprint = InterchangeDesignFingerprint.of(designs);
        List<PlannedInterchangeGeometry> result = new ArrayList<>();
        for (PlanningRegion owner : InterchangeRegionWindow.ownerRegionsAffecting(
                target, MAXIMUM_INTERCHANGE_APPROACH_BLOCKS)) {
            result.addAll(interchangePlanFor(
                    feature, owner, designs, designFingerprint).affecting(target));
        }
        return List.copyOf(result);
    }

    private static RegionalInterchangeGeometryPlan interchangePlanFor(
            LostCityTerrainFeature feature,
            PlanningRegion owner,
            List<InterchangeDesign> designs,
            String designFingerprint) {
        RoadPlanKey key = keyFor(
                feature,
                owner,
                interchangeRulesFingerprint(feature.profile, designFingerprint));
        return PLAN_CACHES.interchangePlan(key, ignored ->
                planInterchanges(feature, key, designs));
    }

    private static RegionalInterchangeGeometryPlan planInterchanges(
            LostCityTerrainFeature feature,
            RoadPlanKey key,
            List<InterchangeDesign> designs) {
        RoadTileLookup roadLookup = (chunk, axis) ->
                planFor(feature, chunk).tileAt(chunk, axis);
        CrossingElevationModel elevations = new CrossingElevationModel(
                HalfBlockElevation.ofWholeBlocks(feature.profile.GROUNDLEVEL),
                LostCityTerrainFeature.FLOORHEIGHT,
                ROAD_STANDARD);
        InterchangeRegionalPlanner planner = new InterchangeRegionalPlanner(
                new RoadCrossingSurveyor(MAXIMUM_INTERCHANGE_APPROACH_BLOCKS, ROAD_STANDARD),
                new InterchangeSelector(designs, ROAD_STANDARD));
        RegionalInterchangePlan selected = planner.plan(key, roadLookup, elevations);
        List<PlannedInterchangeGeometry> geometry = selected.interchanges().stream()
                .map(GEOMETRY_PLANNER::create)
                .toList();

        INTERCHANGE_REGIONS_PLANNED.incrementAndGet();
        REJECTED_CROSSINGS_PLANNED.addAndGet(selected.rejectedCrossings().size());
        if (!geometry.isEmpty()) {
            long previous = SELECTED_INTERCHANGES_PLANNED.getAndAdd(geometry.size());
            if (previous == 0) {
                PlannedInterchangeGeometry first = geometry.getFirst();
                LostCitiesRoadFixes.LOGGER.info(
                        "Calculated interchange generation selected {} at chunk {} in {}",
                        first.layout().design().id(),
                        first.plan().crossing().chunk(),
                        key.dimensionId());
            }
        }
        return new RegionalInterchangeGeometryPlan(
                key, geometry, selected.rejectedCrossings().size());
    }

    private static RoadPlanKey keyFor(
            LostCityTerrainFeature feature,
            PlanningRegion region,
            String rulesFingerprint) {
        return new RoadPlanKey(
                feature.provider.getSeed(),
                feature.provider.getType().location().toString(),
                region,
                rulesFingerprint);
    }

    private static String roadRulesFingerprint(LostCityProfile profile) {
        return "runtime-roads-v1|" + profile.getName()
                + '|' + profile.GROUNDLEVEL
                + '|' + profile.HIGHWAY_DISTANCE_MASK
                + '|' + Float.toHexString(profile.HIGHWAY_MAINPERLIN_SCALE)
                + '|' + Float.toHexString(profile.HIGHWAY_SECONDARYPERLIN_SCALE)
                + '|' + Float.toHexString(profile.HIGHWAY_PERLIN_FACTOR)
                + '|' + profile.HIGHWAY_REQUIRES_TWO_CITIES
                + '|' + profile.HIGHWAY_LEVEL_FROM_CITIES_MODE;
    }

    private static String interchangeRulesFingerprint(
            LostCityProfile profile,
            String designFingerprint) {
        return "runtime-interchanges-v1|"
                + roadRulesFingerprint(profile)
                + '|'
                + designFingerprint;
    }
}
