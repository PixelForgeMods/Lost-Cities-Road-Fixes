package net.austizz.lostcitiesroadfixes.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LostCitiesCompatibilityTest {
    @Test
    void acceptsThePinnedLostCitiesBinaryAndRequiredHookSurface() {
        CompatibilityReport report = LostCitiesCompatibility.inspect();

        assertTrue(report.compatible(), report::diagnostic);
        assertEquals("1.21-8.3.10", report.implementationVersion());
        assertEquals(
                "26db73013028ad724af030aa30cbd8dd62da8b951645b8d02f066c8af083a52f",
                report.sha256());
        assertTrue(report.verifiedSymbols().contains(
                "mcjty.lostcities.worldgen.lost.Highway#getXHighwayLevel"));
        assertTrue(report.verifiedSymbols().contains(
                "mcjty.lostcities.worldgen.LostCityTerrainFeature#fixAfterExplosion"));
        assertTrue(report.verifiedSymbols().contains(
                "mcjty.lostcities.worldgen.LostCityTerrainFeature#generate"));
    }

    @Test
    void declaresARequiredEarlyMixinCompatibilityProbe() throws IOException {
        String mixinConfig = resource("lostcitiesroadfixes.mixins.json");
        String modMetadata = resource("META-INF/neoforge.mods.toml");

        assertTrue(mixinConfig.contains("\"required\": true"));
        assertTrue(mixinConfig.contains(
                "\"plugin\": \"net.austizz.lostcitiesroadfixes.compat.LostCitiesMixinPlugin\""));
        assertTrue(mixinConfig.contains("\"HighwaysMixin\""));
        assertTrue(mixinConfig.contains("\"LostCityTerrainFeatureMixin\""));
        assertTrue(modMetadata.contains("config=\"lostcitiesroadfixes.mixins.json\""));
    }

    private static String resource(String name) throws IOException {
        try (var input = LostCitiesCompatibilityTest.class.getClassLoader().getResourceAsStream(name)) {
            if (input == null) {
                throw new IOException("Missing test resource " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
