package net.austizz.lostcitiesroadfixes.gametest;

import net.austizz.lostcitiesroadfixes.LostCitiesRoadFixes;
import net.austizz.lostcitiesroadfixes.compat.LostCitiesMixinPlugin;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LostCitiesRoadFixes.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DependencyGameTests {
    private DependencyGameTests() {
    }

    @GameTest(template = "empty3x3x3", timeoutTicks = 20)
    public static void exactLostCitiesVersionIsLoaded(GameTestHelper helper) {
        var earlyReport = LostCitiesMixinPlugin.loadedReport();
        if (earlyReport.isEmpty() || !earlyReport.orElseThrow().compatible()) {
            helper.fail("The early Lost Cities Mixin compatibility probe did not complete");
            return;
        }

        var lostCities = ModList.get().getModContainerById("lostcities");
        if (lostCities.isEmpty()) {
            helper.fail("Lost Cities is not loaded");
            return;
        }

        String version = lostCities.orElseThrow().getModInfo().getVersion().toString();
        if (!"1.21-8.3.10".equals(version)) {
            helper.fail("Expected Lost Cities 1.21-8.3.10 but found " + version);
            return;
        }
        helper.succeed();
    }
}
