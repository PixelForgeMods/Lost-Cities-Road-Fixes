package net.austizz.lostcitiesroadfixes.regression;

import net.austizz.lostcitiesroadfixes.road.BlockPoint;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportedGapRegressionTest {
    @Test
    void reproducesTheMiniExplosionThatClipsTheReportedGapChunk() {
        ReportedGapIncident incident = ReportedGapIncident.JULY_2026;

        assertEquals(-6_377_442_428_365_110_436L, incident.seed());
        assertEquals("minecraft:overworld", incident.dimension());
        assertEquals(new ChunkPoint(-64, -140), incident.explosionSourceChunk());
        assertEquals(new ChunkPoint(-64, -139), incident.gapChunk());
        assertEquals(0, incident.cityLevel());

        MiniExplosionSample sample = LostCitiesDamageOracle.sampleMiniExplosion(
                        incident.seed(),
                        incident.explosionSourceChunk(),
                        incident.cityLevel(),
                        MiniExplosionSettings.LOST_CITIES_DEFAULTS)
                .orElseThrow();

        assertEquals(0.029846012592f, sample.chanceRoll());
        assertEquals(7, sample.radius());
        assertEquals(new BlockPoint(-1023, 70, -2225), sample.center());
        assertTrue(sample.intersects(incident.gapChunk()));
    }

    @Test
    void capturesTheSparseLayerThresholdThatDeletesTheRoadAboveIt() {
        assertTrue(LostCitiesCleanupRule.deletesBlocksAbove(15));
        assertFalse(LostCitiesCleanupRule.deletesBlocksAbove(16));
        assertFalse(LostCitiesCleanupRule.deletesBlocksAbove(20));
    }

    @Test
    void requiresReplacementRoadsToRenderAfterLostCitiesCleanup() {
        assertEquals(
                GenerationPhase.AFTER_LOST_CITIES_CLEANUP,
                ReportedGapIncident.JULY_2026.requiredRepairPhase());
    }
}
