package net.austizz.lostcitiesroadfixes.render;

import net.austizz.lostcitiesroadfixes.interchange.geometry.RampCenterlineSample;
import net.austizz.lostcitiesroadfixes.interchange.geometry.RampRoute;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RampSurfaceRasterizer {
    private static final int MINIMUM_SEPARATION_HALF_BLOCKS = Math.multiplyExact(
            RoadDesignStandard.DEFAULT.minimumVehicleClearanceBlocks(), 2);

    public ChunkRoadSurface rasterize(ChunkPoint targetChunk, Collection<RampRoute> routes) {
        Objects.requireNonNull(targetChunk, "targetChunk");
        Objects.requireNonNull(routes, "routes");

        Map<Column, List<CoverageCandidate>> coverage = new HashMap<>();
        int routeOrdinal = 0;
        for (RampRoute route : routes) {
            addRoute(targetChunk, route, routeOrdinal, coverage);
            routeOrdinal++;
        }

        List<RoadSurfaceCell> cells = new ArrayList<>();
        coverage.forEach((column, candidates) ->
                addResolvedColumn(cells, column, candidates));
        return new ChunkRoadSurface(targetChunk, cells);
    }

    private static void addRoute(
            ChunkPoint chunk,
            RampRoute route,
            int routeOrdinal,
            Map<Column, List<CoverageCandidate>> coverage) {
        double halfWidth = route.widthBlocks() / 2.0;
        double maximumDistanceSquared = halfWidth * halfWidth;
        if (!mayOverlap(chunk, route, halfWidth)) {
            return;
        }

        for (int z = chunk.minBlockZ(); z <= chunk.maxBlockZ(); z++) {
            for (int x = chunk.minBlockX(); x <= chunk.maxBlockX(); x++) {
                ClosestPoint closest = closestPoint(route, x + 0.5, z + 0.5);
                if (closest.distanceSquared() >= maximumDistanceSquared) {
                    continue;
                }

                double distance = StrictMath.sqrt(closest.distanceSquared());
                RoadSurfaceRole role = distance >= halfWidth - 1.0
                        ? RoadSurfaceRole.SHOULDER
                        : RoadSurfaceRole.ASPHALT;
                Column column = new Column(x, z);
                coverage.computeIfAbsent(column, ignored -> new ArrayList<>())
                        .add(new CoverageCandidate(
                                route.centerline().elevationAt(
                                        closest.stationBlocks()).halfBlocks(),
                                role,
                                closest.distanceSquared(),
                                routeOrdinal));
            }
        }
    }

    /**
     * Overlapping ramp pavement inside one vehicle-clearance envelope is one
     * connected surface, not a pair of stacked decks. Assign that block column
     * to the nearest route while preserving genuinely separated structures.
     */
    private static void addResolvedColumn(
            List<RoadSurfaceCell> cells,
            Column column,
            List<CoverageCandidate> candidates) {
        List<CoverageCandidate> unresolved = new ArrayList<>(candidates);
        while (!unresolved.isEmpty()) {
            CoverageCandidate anchor = unresolved.stream()
                    .min(Comparator
                            .comparingDouble(CoverageCandidate::distanceSquared)
                            .thenComparingInt(CoverageCandidate::routeOrdinal)
                            .thenComparingInt(CoverageCandidate::elevationHalfBlocks))
                    .orElseThrow();
            List<CoverageCandidate> connectedSurface = unresolved.stream()
                    .filter(candidate -> StrictMath.abs(
                            candidate.elevationHalfBlocks()
                                    - anchor.elevationHalfBlocks())
                            < MINIMUM_SEPARATION_HALF_BLOCKS)
                    .toList();
            cells.add(resolveConnectedSurface(column, anchor, connectedSurface));
            unresolved.removeAll(connectedSurface);
        }
    }

    private static RoadSurfaceCell resolveConnectedSurface(
            Column column,
            CoverageCandidate anchor,
            List<CoverageCandidate> candidates) {
        RoadSurfaceRole role = candidates.stream()
                .map(CoverageCandidate::role)
                .reduce(anchor.role(), RampSurfaceRasterizer::higherPriority);
        return new RoadSurfaceCell(
                new RoadSurfacePosition(
                        column.x(),
                        column.z(),
                        new net.austizz.lostcitiesroadfixes.road.HalfBlockElevation(
                                anchor.elevationHalfBlocks())),
                role);
    }

    private static boolean mayOverlap(ChunkPoint chunk, RampRoute route, double margin) {
        double minimumX = Double.POSITIVE_INFINITY;
        double maximumX = Double.NEGATIVE_INFINITY;
        double minimumZ = Double.POSITIVE_INFINITY;
        double maximumZ = Double.NEGATIVE_INFINITY;
        for (RampCenterlineSample sample : route.centerline().samples()) {
            minimumX = StrictMath.min(minimumX, sample.point().x());
            maximumX = StrictMath.max(maximumX, sample.point().x());
            minimumZ = StrictMath.min(minimumZ, sample.point().z());
            maximumZ = StrictMath.max(maximumZ, sample.point().z());
        }
        return maximumX + margin >= chunk.minBlockX()
                && minimumX - margin <= chunk.maxBlockX() + 1.0
                && maximumZ + margin >= chunk.minBlockZ()
                && minimumZ - margin <= chunk.maxBlockZ() + 1.0;
    }

    private static ClosestPoint closestPoint(RampRoute route, double x, double z) {
        List<RampCenterlineSample> samples = route.centerline().samples();
        ClosestPoint closest = null;
        for (int index = 0; index < samples.size() - 1; index++) {
            RampCenterlineSample start = samples.get(index);
            RampCenterlineSample end = samples.get(index + 1);
            double segmentX = end.point().x() - start.point().x();
            double segmentZ = end.point().z() - start.point().z();
            double lengthSquared = segmentX * segmentX + segmentZ * segmentZ;
            if (lengthSquared == 0.0) {
                continue;
            }
            double fraction = ((x - start.point().x()) * segmentX
                    + (z - start.point().z()) * segmentZ) / lengthSquared;
            fraction = StrictMath.max(0.0, StrictMath.min(1.0, fraction));
            double nearestX = start.point().x() + segmentX * fraction;
            double nearestZ = start.point().z() + segmentZ * fraction;
            double deltaX = x - nearestX;
            double deltaZ = z - nearestZ;
            double distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
            double station = start.stationBlocks()
                    + (end.stationBlocks() - start.stationBlocks()) * fraction;
            ClosestPoint candidate = new ClosestPoint(distanceSquared, station);
            if (closest == null
                    || candidate.distanceSquared() < closest.distanceSquared()
                    || (candidate.distanceSquared() == closest.distanceSquared()
                    && candidate.stationBlocks() < closest.stationBlocks())) {
                closest = candidate;
            }
        }
        if (closest == null) {
            throw new IllegalStateException("Ramp route has no non-zero centerline segment");
        }
        return closest;
    }

    private static RoadSurfaceRole higherPriority(RoadSurfaceRole left, RoadSurfaceRole right) {
        return priority(left) >= priority(right) ? left : right;
    }

    private static int priority(RoadSurfaceRole role) {
        return switch (role) {
            case AT_GRADE_INTERSECTION -> 6;
            case ASPHALT -> 5;
            case WHITE_MARKING, YELLOW_MARKING -> 4;
            case SHOULDER -> 3;
            case MEDIAN -> 2;
        };
    }

    private record ClosestPoint(double distanceSquared, double stationBlocks) {
    }

    private record Column(int x, int z) {
    }

    private record CoverageCandidate(
            int elevationHalfBlocks,
            RoadSurfaceRole role,
            double distanceSquared,
            int routeOrdinal) {
    }
}
