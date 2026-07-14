package net.austizz.lostcitiesroadfixes.config;

import net.austizz.lostcitiesroadfixes.theme.RoadThemeId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoadOperationalSettingsTest {
    private static final RoadThemeId DEFAULT_THEME =
            RoadThemeId.parse("lostcitiesroadfixes:default");

    @Test
    void acceptsOnlyTheSafePublishedBounds() {
        RoadOperationalSettings minimum = new RoadOperationalSettings(
                1, 64, DEFAULT_THEME, false);
        RoadOperationalSettings maximum = new RoadOperationalSettings(
                4, 4_096, DEFAULT_THEME, true);

        assertEquals(1, minimum.maximumGapChunks());
        assertEquals(4_096, maximum.maximumCachedRegions());
        assertThrows(IllegalArgumentException.class, () -> settings(0, 64));
        assertThrows(IllegalArgumentException.class, () -> settings(5, 64));
        assertThrows(IllegalArgumentException.class, () -> settings(1, 63));
        assertThrows(IllegalArgumentException.class, () -> settings(1, 4_097));
        assertThrows(NullPointerException.class, () -> new RoadOperationalSettings(
                1, 64, null, true));
    }

    @Test
    void planningFingerprintChangesOnlyForGeometryAffectingSettings() {
        RoadOperationalSettings baseline = new RoadOperationalSettings(
                4, 512, DEFAULT_THEME, true);
        RoadOperationalSettings gapChanged = new RoadOperationalSettings(
                3, 512, DEFAULT_THEME, true);
        RoadOperationalSettings operationalOnly = new RoadOperationalSettings(
                4,
                1_024,
                RoadThemeId.parse("example:night"),
                false);

        assertNotEquals(baseline.planningFingerprint(), gapChanged.planningFingerprint());
        assertEquals(baseline.planningFingerprint(), operationalOnly.planningFingerprint());
    }

    private static RoadOperationalSettings settings(int gap, int cache) {
        return new RoadOperationalSettings(gap, cache, DEFAULT_THEME, true);
    }
}
