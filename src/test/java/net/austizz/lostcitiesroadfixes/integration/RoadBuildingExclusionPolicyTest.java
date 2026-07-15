package net.austizz.lostcitiesroadfixes.integration;

import mcjty.lostcities.api.MultiPos;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadBuildingExclusionPolicyTest {
    @Test
    void suppressesAnEntireMultiBuildingWhenAnySectionMeetsARoad() {
        Set<ChunkPoint> roads = Set.of(new ChunkPoint(11, 10));

        assertTrue(RoadBuildingExclusionPolicy.intersectsReservedRoadArea(
                new ChunkPoint(10, 10), new MultiPos(0, 0, 2, 2), roads::contains));
        assertTrue(RoadBuildingExclusionPolicy.intersectsReservedRoadArea(
                new ChunkPoint(11, 11), new MultiPos(1, 1, 2, 2), roads::contains));
    }

    @Test
    void reservesOneCompleteChunkOfClearanceAroundRoadSurfaces() {
        Set<ChunkPoint> roads = Set.of(new ChunkPoint(5, 5));

        assertTrue(RoadBuildingExclusionPolicy.intersectsReservedRoadArea(
                new ChunkPoint(6, 5), MultiPos.SINGLE, roads::contains));
        assertFalse(RoadBuildingExclusionPolicy.intersectsReservedRoadArea(
                new ChunkPoint(7, 5), MultiPos.SINGLE, roads::contains));
    }
}
