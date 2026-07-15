package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record InterchangeEnvironment(
        ChunkPoint center,
        int surveyedChunks,
        int cityChunks,
        List<InterchangeBuildingFootprint> buildingFootprints) {
    public InterchangeEnvironment {
        Objects.requireNonNull(center, "center");
        if (surveyedChunks < 0 || cityChunks < 0 || cityChunks > surveyedChunks) {
            throw new IllegalArgumentException("Invalid city survey counts");
        }
        Objects.requireNonNull(buildingFootprints, "buildingFootprints");
        buildingFootprints = buildingFootprints.stream().distinct().sorted().toList();
    }

    public static InterchangeEnvironment empty(ChunkPoint center) {
        return new InterchangeEnvironment(center, 0, 0, List.of());
    }

    public int displacedBuildings(int radiusBlocks) {
        return (int) buildingFootprints.stream()
                .filter(footprint -> footprint.intersectsSquare(center, radiusBlocks))
                .count();
    }

    public int occupiedBuildingChunks() {
        Set<ChunkPoint> occupied = new HashSet<>();
        for (InterchangeBuildingFootprint footprint : buildingFootprints) {
            for (int z = footprint.minChunkZ(); z <= footprint.maxChunkZ(); z++) {
                for (int x = footprint.minChunkX(); x <= footprint.maxChunkX(); x++) {
                    occupied.add(new ChunkPoint(x, z));
                }
            }
        }
        return occupied.size();
    }

    public boolean hasDenseBuildings() {
        return surveyedChunks > 0
                && (long) occupiedBuildingChunks() * 2L >= surveyedChunks;
    }

    public int lightlyOccupiedQuadrants() {
        if (surveyedChunks == 0) {
            return 4;
        }
        @SuppressWarnings("unchecked")
        Set<ChunkPoint>[] occupied = new Set[] {
            new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>()
        };
        for (InterchangeBuildingFootprint footprint : buildingFootprints) {
            for (int z = footprint.minChunkZ(); z <= footprint.maxChunkZ(); z++) {
                for (int x = footprint.minChunkX(); x <= footprint.maxChunkX(); x++) {
                    int east = x >= center.x() ? 1 : 0;
                    int south = z >= center.z() ? 1 : 0;
                    occupied[south * 2 + east].add(new ChunkPoint(x, z));
                }
            }
        }
        int result = 0;
        for (Set<ChunkPoint> quadrant : occupied) {
            if ((long) quadrant.size() * 8L < surveyedChunks) {
                result++;
            }
        }
        return Math.max(1, result);
    }
}
