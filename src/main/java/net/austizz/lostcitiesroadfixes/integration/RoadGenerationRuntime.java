package net.austizz.lostcitiesroadfixes.integration;

import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.api.MultiPos;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.LostCityTerrainFeature;
import net.austizz.lostcitiesroadfixes.LostCitiesRoadFixes;
import net.austizz.lostcitiesroadfixes.config.RoadFixesServerConfig;
import net.austizz.lostcitiesroadfixes.config.RoadOperationalSettings;
import net.austizz.lostcitiesroadfixes.diagnostics.InterchangeExplanation;
import net.austizz.lostcitiesroadfixes.diagnostics.RoadDiagnosticsSnapshot;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesign;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesignFingerprint;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesignResources;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeSelector;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeType;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeLayoutFactory;
import net.austizz.lostcitiesroadfixes.interchange.planning.CrossingElevationModel;
import net.austizz.lostcitiesroadfixes.interchange.planning.InterchangeConflictResolver;
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
import net.austizz.lostcitiesroadfixes.render.ChunkRoadSurface;
import net.austizz.lostcitiesroadfixes.render.ElevatedRoadTile;
import net.austizz.lostcitiesroadfixes.render.MinecraftRoadWriter;
import net.austizz.lostcitiesroadfixes.render.RoadSupportPolicy;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.PlanningGrid;
import net.austizz.lostcitiesroadfixes.road.PlanningRegion;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import net.austizz.lostcitiesroadfixes.theme.CompiledRoadTheme;
import net.austizz.lostcitiesroadfixes.theme.RoadThemeResources;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

public final class RoadGenerationRuntime {
    private static final int MAXIMUM_INTERCHANGE_APPROACH_BLOCKS = 512;
    private static final RoadDesignStandard ROAD_STANDARD = RoadDesignStandard.DEFAULT;
    private static final RuntimePlanCaches<RegionalRoadPlan, RegionalInterchangeGeometryPlan>
            PLAN_CACHES = new RuntimePlanCaches<>();
    private static final InterchangeGeometryPlanner GEOMETRY_PLANNER =
            new InterchangeGeometryPlanner(new InterchangeLayoutFactory(ROAD_STANDARD));
    private static final RuntimeRoadRenderPipeline RENDER_PIPELINE =
            new RuntimeRoadRenderPipeline();
    private static final AtomicLong NATIVE_SUPPRESSIONS = new AtomicLong();
    private static final AtomicLong BUILDING_CHUNK_SUPPRESSIONS = new AtomicLong();
    private static final AtomicLong LATE_RENDER_INVOCATIONS = new AtomicLong();
    private static final AtomicLong INTERCHANGE_REGIONS_PLANNED = new AtomicLong();
    private static final AtomicLong SELECTED_INTERCHANGES_PLANNED = new AtomicLong();
    private static final AtomicLongArray SELECTED_INTERCHANGES_BY_TYPE =
            new AtomicLongArray(InterchangeType.values().length);
    private static final AtomicLong REJECTED_CROSSINGS_PLANNED = new AtomicLong();
    private static final AtomicLong CONFLICTED_CROSSINGS_PLANNED = new AtomicLong();
    private static final AtomicLong INTERCHANGE_RENDER_INVOCATIONS = new AtomicLong();
    private static final AtomicLong STRAIGHT_THROUGH_FALLBACKS = new AtomicLong();

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

    public static long buildingChunkSuppressionCount() {
        return BUILDING_CHUNK_SUPPRESSIONS.get();
    }

    public static long interchangeRegionPlanCount() {
        return INTERCHANGE_REGIONS_PLANNED.get();
    }

    public static long selectedInterchangePlanCount() {
        return SELECTED_INTERCHANGES_PLANNED.get();
    }

    public static Map<InterchangeType, Long> selectedInterchangeFamilyCounts() {
        EnumMap<InterchangeType, Long> result = new EnumMap<>(InterchangeType.class);
        for (InterchangeType type : InterchangeType.values()) {
            result.put(type, SELECTED_INTERCHANGES_BY_TYPE.get(type.ordinal()));
        }
        return Map.copyOf(result);
    }

    public static long rejectedCrossingPlanCount() {
        return REJECTED_CROSSINGS_PLANNED.get();
    }

    public static long conflictedCrossingPlanCount() {
        return CONFLICTED_CROSSINGS_PLANNED.get();
    }

    public static long interchangeRenderInvocationCount() {
        return INTERCHANGE_RENDER_INVOCATIONS.get();
    }

    public static long straightThroughFallbackCount() {
        return STRAIGHT_THROUGH_FALLBACKS.get();
    }

    public static int roadPlanCacheSize() {
        return PLAN_CACHES.roadSize();
    }

    public static int interchangePlanCacheSize() {
        return PLAN_CACHES.interchangeSize();
    }

    public static RoadDiagnosticsSnapshot diagnostics() {
        RoadOperationalSettings settings = RoadFixesServerConfig.settings();
        CompiledRoadTheme resolvedTheme = RoadThemeResources.active(settings.activeThemeId());
        return new RoadDiagnosticsSnapshot(
                true,
                nativeSuppressionCount(),
                buildingChunkSuppressionCount(),
                lateRenderInvocationCount(),
                interchangeRegionPlanCount(),
                selectedInterchangePlanCount(),
                selectedInterchangeFamilyCounts(),
                rejectedCrossingPlanCount(),
                conflictedCrossingPlanCount(),
                interchangeRenderInvocationCount(),
                straightThroughFallbackCount(),
                roadPlanCacheSize(),
                interchangePlanCacheSize(),
                InterchangeDesignResources.repository().snapshot().size(),
                RoadThemeResources.loadedThemeCount(),
                settings.activeThemeId().toString(),
                resolvedTheme.id().toString(),
                settings.maximumGapChunks(),
                settings.maximumCachedRegions(),
                settings.logFirstInterchangeSelection());
    }

    public static void renderAfterLostCities(
            LostCityTerrainFeature feature,
            WorldGenRegion region,
            ChunkAccess chunk) {
        Objects.requireNonNull(feature, "feature");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(chunk, "chunk");
        ChunkPoint target = new ChunkPoint(chunk.getPos().x, chunk.getPos().z);
        RoadOperationalSettings settings = RoadFixesServerConfig.settings();

        try {
            if (LATE_RENDER_INVOCATIONS.incrementAndGet() == 1) {
                LostCitiesRoadFixes.LOGGER.info(
                        "Post-cleanup replacement road rendering is active in {}",
                        feature.provider.getType().location());
            }
            List<ElevatedRoadTile> roads = nearbyRoads(
                    feature.provider, feature.profile, target, settings);
            List<PlannedInterchangeGeometry> interchanges = nearbyInterchanges(
                    feature.provider, feature.profile, target, settings);
            if (!interchanges.isEmpty()) {
                INTERCHANGE_RENDER_INVOCATIONS.incrementAndGet();
            }
            CompiledRoadTheme theme = RoadThemeResources.active(settings.activeThemeId());
            MinecraftRoadWriter writer = new MinecraftRoadWriter(theme.palette());
            RoadSupportPolicy supportPolicy = feature.profile.HIGHWAY_SUPPORTS
                    ? RoadSupportPolicy.enabled(theme.maximumSupportDepthBlocks())
                    : RoadSupportPolicy.disabled();
            ChunkRoadSurface surface = composeRoadSurface(
                    target,
                    roads,
                    interchanges);
            writer.write(chunk, surface, supportPolicy);
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

    public static InterchangeExplanation explainInterchange(
            IDimensionInfo provider,
            LostCityProfile profile,
            ChunkPoint chunk) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(chunk, "chunk");
        RoadOperationalSettings settings = RoadFixesServerConfig.settings();
        List<InterchangeDesign> designs = InterchangeDesignResources.repository().snapshot();
        String designFingerprint = InterchangeDesignFingerprint.of(designs);
        RegionalInterchangeGeometryPlan plan = interchangePlanFor(
                provider,
                profile,
                PlanningGrid.regionFor(chunk),
                designs,
                designFingerprint,
                settings);
        return plan.explanationAt(chunk).orElseGet(() ->
                InterchangeExplanation.none(chunk));
    }

    public static boolean shouldSuppressBuilding(
            ChunkCoord coordinate,
            IDimensionInfo provider,
            LostCityProfile profile,
            MultiPos multiPosition) {
        Objects.requireNonNull(coordinate, "coordinate");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(multiPosition, "multiPosition");
        ChunkPoint current = new ChunkPoint(
                coordinate.chunkX(), coordinate.chunkZ());
        RoadOperationalSettings settings = RoadFixesServerConfig.settings();
        boolean suppress = RoadBuildingExclusionPolicy.intersectsReservedRoadArea(
                current,
                multiPosition,
                candidate -> hasRoadSurface(provider, profile, candidate, settings));
        if (suppress && BUILDING_CHUNK_SUPPRESSIONS.incrementAndGet() == 1) {
            LostCitiesRoadFixes.LOGGER.info(
                    "Building exclusion reservations are active at chunk {}", current);
        }
        return suppress;
    }

    private static boolean hasRoadSurface(
            IDimensionInfo provider,
            LostCityProfile profile,
            ChunkPoint target,
            RoadOperationalSettings settings) {
        List<ElevatedRoadTile> roads = nearbyRoads(provider, profile, target, settings);
        List<PlannedInterchangeGeometry> interchanges = nearbyInterchanges(
                provider, profile, target, settings);
        return !composeRoadSurface(target, roads, interchanges).cells().isEmpty();
    }

    private static ChunkRoadSurface composeRoadSurface(
            ChunkPoint target,
            List<ElevatedRoadTile> roads,
            List<PlannedInterchangeGeometry> interchanges) {
        try {
            return RENDER_PIPELINE.compose(target, roads, interchanges);
        } catch (IllegalArgumentException unsafeInterchange) {
            if (interchanges.isEmpty()) {
                throw unsafeInterchange;
            }
            if (STRAIGHT_THROUGH_FALLBACKS.incrementAndGet() == 1) {
                LostCitiesRoadFixes.LOGGER.error(
                        "Unsafe interchange overlay at chunk {}; preserving graded "
                                + "straight-through highways instead",
                        target,
                        unsafeInterchange);
            }
            return RENDER_PIPELINE.composeStraightThrough(
                    target, roads, interchanges);
        }
    }

    private static List<ElevatedRoadTile> nearbyRoads(
            IDimensionInfo provider,
            LostCityProfile profile,
            ChunkPoint target,
            RoadOperationalSettings settings) {
        List<ElevatedRoadTile> roads = new ArrayList<>();
        for (int offset = -1; offset <= 1; offset++) {
            addIfPresent(provider, profile, new ChunkPoint(target.x(), target.z() + offset),
                    RoadAxis.X, settings, roads);
            addIfPresent(provider, profile, new ChunkPoint(target.x() + offset, target.z()),
                    RoadAxis.Z, settings, roads);
        }
        return List.copyOf(roads);
    }

    private static void addIfPresent(
            IDimensionInfo provider,
            LostCityProfile profile,
            ChunkPoint candidate,
            RoadAxis axis,
            RoadOperationalSettings settings,
            List<ElevatedRoadTile> roads) {
        RegionalRoadPlan plan = planFor(provider, profile, candidate, settings);
        plan.tileAt(candidate, axis).ifPresent(tile -> {
            int blockY = Math.addExact(
                    profile.GROUNDLEVEL,
                    Math.multiplyExact(tile.level(), LostCityTerrainFeature.FLOORHEIGHT));
            roads.add(new ElevatedRoadTile(
                    candidate,
                    axis,
                    HalfBlockElevation.ofWholeBlocks(blockY)));
        });
    }

    private static RegionalRoadPlan planFor(
            IDimensionInfo provider,
            LostCityProfile profile,
            ChunkPoint chunk,
            RoadOperationalSettings settings) {
        RoadPlanKey key = keyFor(
                provider,
                PlanningGrid.regionFor(chunk),
                roadRulesFingerprint(profile, settings));
        return PLAN_CACHES.roadPlan(
                key,
                settings.maximumCachedRegions(),
                ignored -> new ContinuityPlanner(settings.maximumGapChunks()).plan(
                        key,
                        new LostCitiesRoadObservationSource(
                                provider, profile)));
    }

    private static List<PlannedInterchangeGeometry> nearbyInterchanges(
            IDimensionInfo provider,
            LostCityProfile profile,
            ChunkPoint target,
            RoadOperationalSettings settings) {
        List<InterchangeDesign> designs = InterchangeDesignResources.repository().snapshot();
        String designFingerprint = InterchangeDesignFingerprint.of(designs);
        List<PlannedInterchangeGeometry> result = new ArrayList<>();
        for (PlanningRegion owner : InterchangeRegionWindow.ownerRegionsAffecting(
                target, MAXIMUM_INTERCHANGE_APPROACH_BLOCKS)) {
            result.addAll(interchangePlanFor(
                    provider, profile, owner, designs, designFingerprint, settings)
                    .affecting(target));
        }
        return List.copyOf(result);
    }

    private static RegionalInterchangeGeometryPlan interchangePlanFor(
            IDimensionInfo provider,
            LostCityProfile profile,
            PlanningRegion owner,
            List<InterchangeDesign> designs,
            String designFingerprint,
            RoadOperationalSettings settings) {
        RoadPlanKey key = keyFor(
                provider,
                owner,
                interchangeRulesFingerprint(
                        profile, designFingerprint, settings));
        return PLAN_CACHES.interchangePlan(
                key,
                settings.maximumCachedRegions(),
                ignored -> planInterchanges(provider, profile, key, designs, settings));
    }

    private static RegionalInterchangeGeometryPlan planInterchanges(
            IDimensionInfo provider,
            LostCityProfile profile,
            RoadPlanKey key,
            List<InterchangeDesign> designs,
            RoadOperationalSettings settings) {
        RoadTileLookup roadLookup = (chunk, axis) ->
                planFor(provider, profile, chunk, settings).tileAt(chunk, axis);
        CrossingElevationModel elevations = new CrossingElevationModel(
                HalfBlockElevation.ofWholeBlocks(profile.GROUNDLEVEL),
                LostCityTerrainFeature.FLOORHEIGHT,
                ROAD_STANDARD);
        InterchangeRegionalPlanner planner = new InterchangeRegionalPlanner(
                new RoadCrossingSurveyor(
                        MAXIMUM_INTERCHANGE_APPROACH_BLOCKS,
                        ROAD_STANDARD,
                        new LostCitiesCrossingEnvironmentLookup(provider)),
                new InterchangeSelector(designs, ROAD_STANDARD),
                new InterchangeConflictResolver(ROAD_STANDARD));
        RegionalInterchangePlan selected = planner.plan(key, roadLookup, elevations);
        List<PlannedInterchangeGeometry> geometry = selected.interchanges().stream()
                .map(GEOMETRY_PLANNER::create)
                .toList();
        List<InterchangeExplanation> explanations = new ArrayList<>(
                selected.interchanges().size()
                        + selected.rejectedCrossings().size()
                        + selected.conflictedCrossings().size());
        selected.interchanges().forEach(interchange ->
                explanations.add(InterchangeExplanation.selected(interchange)));
        selected.rejectedCrossings().forEach(rejected ->
                explanations.add(InterchangeExplanation.rejected(rejected)));
        selected.conflictedCrossings().forEach(conflict ->
                explanations.add(InterchangeExplanation.conflicted(conflict)));

        INTERCHANGE_REGIONS_PLANNED.incrementAndGet();
        REJECTED_CROSSINGS_PLANNED.addAndGet(selected.rejectedCrossings().size());
        CONFLICTED_CROSSINGS_PLANNED.addAndGet(selected.conflictedCrossings().size());
        if (!geometry.isEmpty()) {
            long previous = SELECTED_INTERCHANGES_PLANNED.getAndAdd(geometry.size());
            geometry.forEach(interchange -> SELECTED_INTERCHANGES_BY_TYPE.incrementAndGet(
                    interchange.layout().design().type().ordinal()));
            if (previous == 0 && settings.logFirstInterchangeSelection()) {
                PlannedInterchangeGeometry first = geometry.getFirst();
                LostCitiesRoadFixes.LOGGER.info(
                        "Calculated interchange generation selected {} at chunk {} in {}",
                        first.layout().design().id(),
                        first.plan().crossing().chunk(),
                        key.dimensionId());
            }
        }
        return new RegionalInterchangeGeometryPlan(
                key,
                geometry,
                selected.rejectedCrossings().size(),
                selected.conflictedCrossings().size(),
                explanations);
    }

    private static RoadPlanKey keyFor(
            IDimensionInfo provider,
            PlanningRegion region,
            String rulesFingerprint) {
        return new RoadPlanKey(
                provider.getSeed(),
                provider.getType().location().toString(),
                region,
                rulesFingerprint);
    }

    private static String roadRulesFingerprint(
            LostCityProfile profile,
            RoadOperationalSettings settings) {
        return "runtime-roads-v1|" + profile.getName()
                + '|' + profile.GROUNDLEVEL
                + '|' + profile.HIGHWAY_DISTANCE_MASK
                + '|' + Float.toHexString(profile.HIGHWAY_MAINPERLIN_SCALE)
                + '|' + Float.toHexString(profile.HIGHWAY_SECONDARYPERLIN_SCALE)
                + '|' + Float.toHexString(profile.HIGHWAY_PERLIN_FACTOR)
                + '|' + profile.HIGHWAY_REQUIRES_TWO_CITIES
                + '|' + profile.HIGHWAY_LEVEL_FROM_CITIES_MODE
                + '|' + settings.planningFingerprint();
    }

    private static String interchangeRulesFingerprint(
            LostCityProfile profile,
            String designFingerprint,
            RoadOperationalSettings settings) {
        return "runtime-interchanges-v4|professional-terminals-v1|site-envelope-v2|"
                + "compiled-corridor-reservations-v1|physical-structure-levels-v1|"
                + roadRulesFingerprint(profile, settings)
                + '|'
                + designFingerprint;
    }
}
