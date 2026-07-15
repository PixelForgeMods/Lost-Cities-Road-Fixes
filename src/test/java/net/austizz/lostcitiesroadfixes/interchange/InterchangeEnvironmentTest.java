package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterchangeEnvironmentTest {
    @Test
    void deduplicatesMultiChunkBuildingsAndCountsEachFootprintOnce() {
        InterchangeBuildingFootprint multi = new InterchangeBuildingFootprint(
                -1, 0, -1, 0, 2);
        InterchangeEnvironment environment = new InterchangeEnvironment(
                new ChunkPoint(0, 0),
                16,
                12,
                List.of(multi, multi, new InterchangeBuildingFootprint(20, 20, 20, 20, 1)));

        assertEquals(2, environment.buildingFootprints().size());
        assertEquals(1, environment.displacedBuildings(64));
        assertEquals(5, environment.occupiedBuildingChunks());
        assertFalse(environment.hasDenseBuildings());
    }

    @Test
    void reportsDenseBuildingOccupancyAtHalfTheSurveyedChunks() {
        InterchangeEnvironment environment = new InterchangeEnvironment(
                new ChunkPoint(0, 0),
                8,
                8,
                List.of(new InterchangeBuildingFootprint(-1, 0, -1, 0, 3)));

        assertTrue(environment.hasDenseBuildings());
    }
}
