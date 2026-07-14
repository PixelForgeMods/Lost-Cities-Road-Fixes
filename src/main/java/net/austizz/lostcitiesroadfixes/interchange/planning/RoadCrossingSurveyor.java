package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.interchange.JunctionForm;
import net.austizz.lostcitiesroadfixes.interchange.TrafficDemand;
import net.austizz.lostcitiesroadfixes.interchange.layout.ApproachDirection;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadTile;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RoadCrossingSurveyor {
    public static final int MAXIMUM_RADIUS_BLOCKS = 128;

    private final int maximumApproachBlocks;
    private final int maximumArmChunks;
    private final RoadDesignStandard standard;

    public RoadCrossingSurveyor(int maximumApproachBlocks, RoadDesignStandard standard) {
        if (maximumApproachBlocks < 1) {
            throw new IllegalArgumentException("Maximum approach must be positive");
        }
        this.maximumApproachBlocks = maximumApproachBlocks;
        this.maximumArmChunks = Math.floorDiv(maximumApproachBlocks + 15, 16);
        this.standard = Objects.requireNonNull(standard, "standard");
    }

    public Optional<DetectedRoadCrossing> survey(
            ChunkPoint chunk,
            RoadTileLookup roads,
            CrossingElevationModel elevations,
            long worldSeed) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(roads, "roads");
        Objects.requireNonNull(elevations, "elevations");

        Optional<RoadTile> xRoad = roads.tileAt(chunk, RoadAxis.X);
        Optional<RoadTile> zRoad = roads.tileAt(chunk, RoadAxis.Z);
        if (xRoad.isEmpty() || zRoad.isEmpty() || xRoad.get().level() == zRoad.get().level()) {
            return Optional.empty();
        }

        Map<ApproachDirection, Integer> armRuns = new EnumMap<>(ApproachDirection.class);
        putArm(armRuns, ApproachDirection.WEST,
                armRun(chunk, -1, 0, RoadAxis.X, xRoad.get().level(), roads));
        putArm(armRuns, ApproachDirection.EAST,
                armRun(chunk, 1, 0, RoadAxis.X, xRoad.get().level(), roads));
        putArm(armRuns, ApproachDirection.NORTH,
                armRun(chunk, 0, -1, RoadAxis.Z, zRoad.get().level(), roads));
        putArm(armRuns, ApproachDirection.SOUTH,
                armRun(chunk, 0, 1, RoadAxis.Z, zRoad.get().level(), roads));
        if (armRuns.size() < 3) {
            return Optional.empty();
        }

        JunctionForm form = armRuns.size() == 3
                ? JunctionForm.THREE_WAY
                : JunctionForm.FOUR_WAY;
        int approach = armRuns.values().stream().mapToInt(Integer::intValue).min().orElseThrow();
        int radius = Math.min(MAXIMUM_RADIUS_BLOCKS, Math.max(1, approach / 2));
        TrafficDemand demand = approach >= 192
                ? TrafficDemand.HIGH
                : approach >= 128 ? TrafficDemand.REGIONAL : TrafficDemand.LOCAL;
        int maximumLevels = radius >= 96 ? 4 : radius >= 64 ? 3 : 2;
        boolean loopsAllowed = radius >= 64;
        boolean requireFreeFlow = demand == TrafficDemand.HIGH;
        CrossingDecks decks = elevations.planDecks(xRoad.get().level(), zRoad.get().level());
        int separationHalfBlocks = decks.upperPlannedDeck().halfBlocks()
                - decks.lowerPlannedDeck().halfBlocks();
        if (separationHalfBlocks < standard.minimumVehicleClearanceBlocks() * 2) {
            throw new IllegalStateException("Planned crossing does not provide vehicle clearance");
        }

        return Optional.of(new DetectedRoadCrossing(
                chunk,
                form,
                xRoad.get().level(),
                zRoad.get().level(),
                EnumSet.copyOf(armRuns.keySet()),
                approach,
                radius,
                form == JunctionForm.FOUR_WAY ? 4 : 3,
                demand,
                maximumLevels,
                loopsAllowed,
                requireFreeFlow,
                decks,
                siteSeed(worldSeed, chunk, xRoad.get().level(), zRoad.get().level())));
    }

    private int armRun(
            ChunkPoint origin,
            int stepX,
            int stepZ,
            RoadAxis axis,
            int level,
            RoadTileLookup roads) {
        int chunks = 0;
        for (int step = 1; step <= maximumArmChunks; step++) {
            ChunkPoint candidate = new ChunkPoint(
                    Math.addExact(origin.x(), Math.multiplyExact(stepX, step)),
                    Math.addExact(origin.z(), Math.multiplyExact(stepZ, step)));
            Optional<RoadTile> road = roads.tileAt(candidate, axis);
            if (road.isEmpty() || road.get().level() != level) {
                break;
            }
            chunks++;
        }
        return Math.min(maximumApproachBlocks, Math.multiplyExact(chunks, 16));
    }

    private static void putArm(
            Map<ApproachDirection, Integer> arms,
            ApproachDirection direction,
            int runBlocks) {
        if (runBlocks > 0) {
            arms.put(direction, runBlocks);
        }
    }

    private static long siteSeed(long worldSeed, ChunkPoint chunk, int xLevel, int zLevel) {
        long value = worldSeed;
        value ^= (long) chunk.x() * 0x9e3779b97f4a7c15L;
        value ^= (long) chunk.z() * 0xc2b2ae3d27d4eb4fL;
        value ^= ((long) xLevel << 32) ^ Integer.toUnsignedLong(zLevel);
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
