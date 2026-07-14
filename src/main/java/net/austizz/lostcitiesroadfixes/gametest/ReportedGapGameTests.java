package net.austizz.lostcitiesroadfixes.gametest;

import net.austizz.lostcitiesroadfixes.LostCitiesRoadFixes;
import net.austizz.lostcitiesroadfixes.regression.GenerationPhase;
import net.austizz.lostcitiesroadfixes.regression.LostCitiesCleanupRule;
import net.austizz.lostcitiesroadfixes.regression.LostCitiesDamageOracle;
import net.austizz.lostcitiesroadfixes.regression.MiniExplosionSettings;
import net.austizz.lostcitiesroadfixes.regression.ReportedGapIncident;
import net.austizz.lostcitiesroadfixes.road.BlockPoint;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LostCitiesRoadFixes.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReportedGapGameTests {
    private ReportedGapGameTests() {
    }

    @GameTest(template = "empty3x3x3", timeoutTicks = 20)
    public static void reportedGapRemainsReproducibleAtRuntime(GameTestHelper helper) {
        ReportedGapIncident incident = ReportedGapIncident.JULY_2026;
        var sample = LostCitiesDamageOracle.sampleMiniExplosion(
                incident.seed(),
                incident.explosionSourceChunk(),
                incident.cityLevel(),
                MiniExplosionSettings.LOST_CITIES_DEFAULTS);

        var explosion = sample.orElse(null);

        if (explosion == null
                || explosion.radius() != 7
                || !explosion.center().equals(new BlockPoint(-1023, 70, -2225))
                || !explosion.intersects(incident.gapChunk())
                || !LostCitiesCleanupRule.deletesBlocksAbove(15)
                || incident.requiredRepairPhase() != GenerationPhase.AFTER_LOST_CITIES_CLEANUP) {
            helper.fail("The reported Lost Cities road-gap regression no longer reproduces");
            return;
        }

        helper.succeed();
    }
}
