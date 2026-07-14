package net.austizz.lostcitiesroadfixes.road;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadDomainContractsTest {
    private static final RoadDesignStandard STANDARD = RoadDesignStandard.DEFAULT;

    @Test
    void ownsChunksInFloorDividedRegionsWithAHalo() {
        PlanningRegion origin = PlanningGrid.regionFor(new ChunkPoint(63, 63));
        PlanningRegion negative = PlanningGrid.regionFor(new ChunkPoint(-1, -65));

        assertEquals(new PlanningRegion(0, 0), origin);
        assertEquals(new PlanningRegion(-1, -2), negative);
        assertTrue(origin.owns(new ChunkPoint(0, 0)));
        assertTrue(origin.owns(new ChunkPoint(63, 63)));
        assertFalse(origin.owns(new ChunkPoint(64, 63)));
        assertEquals(-32, origin.planningMinChunkX());
        assertEquals(95, origin.planningMaxChunkX());
        assertEquals(64, PlanningGrid.REGION_SIZE_CHUNKS);
        assertEquals(32, PlanningGrid.HALO_CHUNKS);
    }

    @Test
    void definesTheApprovedTwoPlusTwoCrossSection() {
        RoadCrossSection crossSection = STANDARD.arterialCrossSection();

        assertEquals(2, crossSection.lanesPerDirection());
        assertEquals(7, crossSection.laneWidthBlocks());
        assertEquals(2, crossSection.medianWidthBlocks());
        assertEquals(1, crossSection.shoulderWidthBlocks());
        assertEquals(32, crossSection.totalWidthBlocks());
    }

    @Test
    void enforcesHalfSlabGradeCurveAndClearanceLimits() {
        assertTrue(STANDARD.acceptsGrade(new RoadGrade(1, 8)));
        assertTrue(STANDARD.acceptsGrade(new RoadGrade(2, 16)));
        assertFalse(STANDARD.acceptsGrade(new RoadGrade(1, 7)));

        STANDARD.requireCurveRadius(RoadKind.ARTERIAL, 32);
        STANDARD.requireCurveRadius(RoadKind.RAMP, 24);
        STANDARD.requireVehicleClearance(7);

        var arterialError = assertThrows(
                IllegalArgumentException.class,
                () -> STANDARD.requireCurveRadius(RoadKind.ARTERIAL, 31));
        var rampError = assertThrows(
                IllegalArgumentException.class,
                () -> STANDARD.requireCurveRadius(RoadKind.RAMP, 23));
        var clearanceError = assertThrows(
                IllegalArgumentException.class,
                () -> STANDARD.requireVehicleClearance(6));

        assertTrue(arterialError.getMessage().contains("32"));
        assertTrue(rampError.getMessage().contains("24"));
        assertTrue(clearanceError.getMessage().contains("7"));
        assertEquals(10, STANDARD.preferredDeckSeparationBlocks());
    }

    @Test
    void representsRoadElevationInHalfBlocksWithoutRounding() {
        HalfBlockElevation elevation = new HalfBlockElevation(143);

        assertEquals(71.5, elevation.blocks());
        assertEquals(71, elevation.floorBlockY());
        assertEquals(new HalfBlockElevation(144), elevation.plusHalfBlocks(1));
        assertEquals(-1, new HalfBlockElevation(-1).floorBlockY());
    }

    @Test
    void rejectsInvalidStandardsWithActionableMessages() {
        var crossSectionError = assertThrows(
                IllegalArgumentException.class,
                () -> new RoadDesignStandard(
                        new RoadCrossSection(2, 6, 2, 1), 1, 8, 32, 24, 7, 10));
        var gradeError = assertThrows(
                IllegalArgumentException.class,
                () -> new RoadGrade(-1, 8));

        assertTrue(crossSectionError.getMessage().contains("32 blocks"));
        assertTrue(gradeError.getMessage().contains("negative"));
    }
}
