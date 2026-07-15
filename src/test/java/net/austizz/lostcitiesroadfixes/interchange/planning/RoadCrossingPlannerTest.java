package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeSelector;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeType;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeEnvironment;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeBuildingFootprint;
import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.TrafficDemand;
import net.austizz.lostcitiesroadfixes.interchange.layout.ApproachDirection;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeLayoutFactory;
import net.austizz.lostcitiesroadfixes.interchange.render.InterchangeGeometryPlanner;
import net.austizz.lostcitiesroadfixes.planning.RoadPlanKey;
import net.austizz.lostcitiesroadfixes.planning.continuity.ChunkBounds;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadTile;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadTileKey;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadTileOrigin;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.PlanningGrid;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadCrossingPlannerTest {
    private static final RoadDesignStandard STANDARD = RoadDesignStandard.DEFAULT;
    private static final CrossingElevationModel ELEVATIONS = new CrossingElevationModel(
            new HalfBlockElevation(140), 6, STANDARD);

    @Test
    void surveysFourWayCrossingAndExpandsNativeSixBlockDeckSpacing() {
        Map<RoadTileKey, RoadTile> roads = crossing(new ChunkPoint(0, 0), 16, 0, 1, false);
        RoadCrossingSurveyor surveyor = new RoadCrossingSurveyor(256, STANDARD);

        DetectedRoadCrossing crossing = surveyor.survey(
                new ChunkPoint(0, 0), lookup(roads), ELEVATIONS, 42L).orElseThrow();

        assertEquals(JunctionForm.FOUR_WAY, crossing.form());
        assertEquals(4, crossing.approaches().size());
        assertEquals(256, crossing.approachRunBlocks());
        assertEquals(192, crossing.availableRadiusBlocks());
        assertEquals(TrafficDemand.REGIONAL, crossing.demand());
        assertEquals(new HalfBlockElevation(140), crossing.decks().nativeX());
        assertEquals(new HalfBlockElevation(152), crossing.decks().nativeZ());
        assertEquals(new HalfBlockElevation(140), crossing.decks().plannedX());
        assertEquals(new HalfBlockElevation(160), crossing.decks().plannedZ());
        assertEquals(10.0, crossing.selectionSite().upperDeck().blocks()
                - crossing.selectionSite().lowerDeck().blocks());
    }

    @Test
    void surveyedDemandDoesNotChangeWithWorldSeed() {
        Map<RoadTileKey, RoadTile> roads = crossing(new ChunkPoint(0, 0), 16, 0, 1, false);
        RoadCrossingSurveyor surveyor = new RoadCrossingSurveyor(256, STANDARD);

        DetectedRoadCrossing first = surveyor.survey(
                new ChunkPoint(0, 0), lookup(roads), ELEVATIONS, 1L).orElseThrow();
        DetectedRoadCrossing second = surveyor.survey(
                new ChunkPoint(0, 0), lookup(roads), ELEVATIONS, Long.MAX_VALUE).orElseThrow();

        assertEquals(first.demand(), second.demand());
        assertEquals(first.availableQuadrants(), second.availableQuadrants());
        assertEquals(first.maximumStructureLevels(), second.maximumStructureLevels());
        assertEquals(first.loopRampsAllowed(), second.loopRampsAllowed());
        assertEquals(TrafficDemand.REGIONAL, first.demand());
        assertEquals(2, first.maximumStructureLevels());
    }

    @Test
    void denseBuildingSurveyUpgradesMeasuredDemandOneClass() {
        Map<RoadTileKey, RoadTile> roads = crossing(new ChunkPoint(0, 0), 16, 0, 1, false);
        List<InterchangeBuildingFootprint> buildings = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            buildings.add(new InterchangeBuildingFootprint(index, index, 0, 0, 0));
        }
        CrossingEnvironmentLookup environment = (center, radius) ->
                new InterchangeEnvironment(center, 16, 16, buildings);
        RoadCrossingSurveyor surveyor = new RoadCrossingSurveyor(
                256, STANDARD, environment);

        DetectedRoadCrossing crossing = surveyor.survey(
                new ChunkPoint(0, 0), lookup(roads), ELEVATIONS, 42L).orElseThrow();

        assertEquals(TrafficDemand.HIGH, crossing.demand());
    }

    @Test
    void distinguishesAThreeWayEndpointFromAFourWayCrossing() {
        Map<RoadTileKey, RoadTile> roads = crossing(new ChunkPoint(0, 0), 16, 0, 1, true);

        DetectedRoadCrossing crossing = new RoadCrossingSurveyor(256, STANDARD)
                .survey(new ChunkPoint(0, 0), lookup(roads), ELEVATIONS, 42L)
                .orElseThrow();

        assertEquals(JunctionForm.THREE_WAY, crossing.form());
        assertEquals(
                java.util.EnumSet.of(
                        ApproachDirection.WEST,
                        ApproachDirection.EAST,
                        ApproachDirection.SOUTH),
                crossing.approaches());
    }

    @Test
    void ignoresSameLevelAndTwoArmCrossings() {
        RoadCrossingSurveyor surveyor = new RoadCrossingSurveyor(256, STANDARD);
        Map<RoadTileKey, RoadTile> sameLevel = crossing(
                new ChunkPoint(0, 0), 16, 1, 1, false);
        Map<RoadTileKey, RoadTile> twoArm = crossing(
                new ChunkPoint(0, 0), 16, 0, 1, false);
        twoArm.entrySet().removeIf(entry -> entry.getKey().axis() == RoadAxis.Z
                && entry.getKey().chunk().z() != 0);

        assertTrue(surveyor.survey(
                new ChunkPoint(0, 0), lookup(sameLevel), ELEVATIONS, 1L).isEmpty());
        assertTrue(surveyor.survey(
                new ChunkPoint(0, 0), lookup(twoArm), ELEVATIONS, 1L).isEmpty());
    }

    @Test
    void selectsTheBestFeasibleDesignAndRetainsShortGradeRejections() {
        Map<RoadTileKey, RoadTile> roads = crossing(new ChunkPoint(0, 0), 16, 0, 1, false);
        Map<RoadTileKey, RoadTile> steepRoads = crossing(
                new ChunkPoint(0, 0), 16, 0, 3, false);
        RoadPlanKey key = keyFor(new ChunkPoint(0, 0));
        ChunkBounds crossingOnly = new ChunkBounds(0, 0, 0, 0);

        RegionalInterchangePlan feasible = planner(256).plan(
                key, crossingOnly, lookup(roads), ELEVATIONS);
        RegionalInterchangePlan tooShort = planner(128).plan(
                key, crossingOnly, lookup(steepRoads), ELEVATIONS);

        assertEquals(1, feasible.interchanges().size());
        assertEquals(InterchangeType.DIAMOND,
                feasible.interchanges().getFirst().decision().selected().orElseThrow().type());
        assertTrue(feasible.rejectedCrossings().isEmpty());

        assertTrue(tooShort.interchanges().isEmpty());
        assertEquals(1, tooShort.rejectedCrossings().size());
        assertTrue(tooShort.rejectedCrossings().getFirst().decision().diagnostic()
                .contains("turning-ramp grade requires 288"));
    }

    @Test
    void selectsDriveableInterchangeForReportedEighteenBlockCrossing() {
        Map<RoadTileKey, RoadTile> roads = crossing(new ChunkPoint(0, 0), 32, 0, 3, false);
        RoadPlanKey key = keyFor(new ChunkPoint(0, 0));
        ChunkBounds crossingOnly = new ChunkBounds(0, 0, 0, 0);

        RegionalInterchangePlan plan = planner(512).plan(
                key, crossingOnly, lookup(roads), ELEVATIONS);

        assertEquals(1, plan.interchanges().size(), () ->
                plan.rejectedCrossings().isEmpty()
                        ? plan.conflictedCrossings().toString()
                        : plan.rejectedCrossings().getFirst().decision().diagnostic());
        assertEquals(InterchangeType.PARTIAL_CLOVERLEAF,
                plan.interchanges().getFirst().decision().selected().orElseThrow().type());
        assertTrue(plan.rejectedCrossings().isEmpty());
    }

    @Test
    void longFourWayRoadSelectionIsMeasuredAndSeedIndependent() {
        Map<RoadTileKey, RoadTile> roads = crossing(new ChunkPoint(0, 0), 32, 0, 1, false);
        RoadCrossingSurveyor surveyor = new RoadCrossingSurveyor(512, STANDARD);
        InterchangeSelector selector = InterchangeSelector.withBuiltIns();
        Set<InterchangeType> selected = EnumSet.noneOf(InterchangeType.class);
        Map<InterchangeType, PlannedInterchange> firstOfEachFamily =
                new EnumMap<>(InterchangeType.class);

        for (long seed = 0; seed < 512; seed++) {
            DetectedRoadCrossing crossing = surveyor.survey(
                    new ChunkPoint(0, 0), lookup(roads), ELEVATIONS, seed).orElseThrow();
            var decision = selector.select(crossing.selectionSite());
            decision.selected().ifPresent(design -> {
                selected.add(design.type());
                firstOfEachFamily.putIfAbsent(
                        design.type(), new PlannedInterchange(crossing, decision));
            });
        }

        Set<InterchangeType> expected = EnumSet.of(InterchangeType.CLOVERLEAF);
        assertEquals(expected, selected);

        InterchangeGeometryPlanner geometryPlanner = new InterchangeGeometryPlanner(
                new InterchangeLayoutFactory(STANDARD));
        firstOfEachFamily.forEach((type, plan) -> assertEquals(
                type,
                geometryPlanner.create(plan).layout().design().type()));
    }

    @Test
    void emitsOnlyCrossingsOwnedByThePlanRegion() {
        ChunkPoint boundary = new ChunkPoint(64, 0);
        Map<RoadTileKey, RoadTile> roads = crossing(boundary, 16, 0, 1, false);
        ChunkBounds crossingOnly = new ChunkBounds(64, 64, 0, 0);

        RegionalInterchangePlan wrongOwner = planner(256).plan(
                keyFor(new ChunkPoint(63, 0)), crossingOnly, lookup(roads), ELEVATIONS);
        RegionalInterchangePlan owner = planner(256).plan(
                keyFor(boundary), crossingOnly, lookup(roads), ELEVATIONS);

        assertTrue(wrongOwner.interchanges().isEmpty());
        assertTrue(wrongOwner.rejectedCrossings().isEmpty());
        assertEquals(1, owner.interchanges().size());
        assertEquals(boundary, owner.interchanges().getFirst().crossing().chunk());
    }

    @Test
    void planningDoesNotDependOnLookupInsertionOrder() {
        Map<RoadTileKey, RoadTile> ordered = crossing(new ChunkPoint(-1, -1), 16, 0, 1, false);
        List<Map.Entry<RoadTileKey, RoadTile>> reversed = new ArrayList<>(ordered.entrySet());
        Collections.reverse(reversed);
        Map<RoadTileKey, RoadTile> reverseMap = new LinkedHashMap<>();
        reversed.forEach(entry -> reverseMap.put(entry.getKey(), entry.getValue()));
        RoadPlanKey key = keyFor(new ChunkPoint(-1, -1));
        ChunkBounds crossingOnly = new ChunkBounds(-1, -1, -1, -1);

        RegionalInterchangePlan first = planner(256).plan(
                key, crossingOnly, lookup(ordered), ELEVATIONS);
        RegionalInterchangePlan second = planner(256).plan(
                key, crossingOnly, lookup(reverseMap), ELEVATIONS);

        assertEquals(first, second);
        assertFalse(first.interchanges().isEmpty());
    }

    @Test
    void denseEightChunkCrossingsProduceOneSafeCoreAndOneConflict() {
        ChunkPoint firstCrossing = new ChunkPoint(0, 0);
        ChunkPoint secondCrossing = new ChunkPoint(4, 0);
        Map<RoadTileKey, RoadTile> roads = crossing(firstCrossing, 16, 0, 1, false);
        roads.putAll(crossing(secondCrossing, 16, 0, 1, false));

        RegionalInterchangePlan plan = planner(256).plan(
                keyFor(firstCrossing),
                new ChunkBounds(0, 4, 0, 0),
                lookup(roads),
                ELEVATIONS);

        assertEquals(1, plan.interchanges().size());
        assertEquals(1, plan.conflictedCrossings().size());
        assertTrue(plan.rejectedCrossings().isEmpty());
        assertEquals(
                plan.interchanges().getFirst().crossing().chunk(),
                plan.conflictedCrossings().getFirst().blockingCrossing());
    }

    @Test
    void denseConflictOwnershipAgreesAcrossPositiveAndNegativeRegionBoundaries() {
        assertBoundaryConflict(new ChunkPoint(63, 0), new ChunkPoint(64, 0));
        assertBoundaryConflict(new ChunkPoint(-65, 0), new ChunkPoint(-64, 0));
    }

    private static InterchangeRegionalPlanner planner(int maximumApproachBlocks) {
        return new InterchangeRegionalPlanner(
                new RoadCrossingSurveyor(maximumApproachBlocks, STANDARD),
                InterchangeSelector.withBuiltIns(),
                new InterchangeConflictResolver(STANDARD));
    }

    private static void assertBoundaryConflict(ChunkPoint first, ChunkPoint second) {
        Map<RoadTileKey, RoadTile> roads = crossing(first, 16, 0, 1, false);
        roads.putAll(crossing(second, 16, 0, 1, false));

        RegionalInterchangePlan firstOwner = planner(256).plan(
                keyFor(first),
                new ChunkBounds(first.x(), first.x(), first.z(), first.z()),
                lookup(roads),
                ELEVATIONS);
        RegionalInterchangePlan secondOwner = planner(256).plan(
                keyFor(second),
                new ChunkBounds(second.x(), second.x(), second.z(), second.z()),
                lookup(roads),
                ELEVATIONS);

        assertEquals(1,
                firstOwner.interchanges().size() + secondOwner.interchanges().size());
        assertEquals(1,
                firstOwner.conflictedCrossings().size()
                        + secondOwner.conflictedCrossings().size());
    }

    private static RoadPlanKey keyFor(ChunkPoint chunk) {
        return new RoadPlanKey(
                42L,
                "minecraft:overworld",
                PlanningGrid.regionFor(chunk),
                "crossing-test");
    }

    private static RoadTileLookup lookup(Map<RoadTileKey, RoadTile> roads) {
        return (chunk, axis) -> java.util.Optional.ofNullable(
                roads.get(new RoadTileKey(chunk, axis)));
    }

    private static Map<RoadTileKey, RoadTile> crossing(
            ChunkPoint center,
            int armChunks,
            int xLevel,
            int zLevel,
            boolean omitNorth) {
        Map<RoadTileKey, RoadTile> roads = new LinkedHashMap<>();
        for (int offset = -armChunks; offset <= armChunks; offset++) {
            put(roads, new ChunkPoint(center.x() + offset, center.z()), RoadAxis.X, xLevel);
            if (!omitNorth || offset >= 0) {
                put(roads, new ChunkPoint(center.x(), center.z() + offset), RoadAxis.Z, zLevel);
            }
        }
        return roads;
    }

    private static void put(
            Map<RoadTileKey, RoadTile> roads,
            ChunkPoint chunk,
            RoadAxis axis,
            int level) {
        RoadTile tile = new RoadTile(
                new RoadTileKey(chunk, axis), level, RoadTileOrigin.OBSERVED);
        roads.put(tile.key(), tile);
    }
}
