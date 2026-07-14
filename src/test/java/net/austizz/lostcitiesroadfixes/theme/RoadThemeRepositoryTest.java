package net.austizz.lostcitiesroadfixes.theme;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoadThemeRepositoryTest {
    @Test
    void publishesImmutableOrderedSnapshotsAndActivatesTheReplaceableDefault() {
        RoadTheme builtIn = RoadThemeCatalogue.defaultTheme();
        RoadTheme replacement = RoadThemeJson.parse(
                RoadThemeCatalogue.DEFAULT_ID,
                RoadThemeJsonTest.validJson());
        RoadTheme custom = RoadThemeJson.parse(
                RoadThemeId.parse("example:custom"),
                RoadThemeJsonTest.validJson());
        RoadThemeRepository first = new RoadThemeRepository(List.of(builtIn));
        List<RoadTheme> before = first.snapshot();

        first.replaceCustom(inOrder(custom, replacement));
        RoadThemeRepository second = new RoadThemeRepository(List.of(builtIn));
        second.replaceCustom(inOrder(replacement, custom));

        assertEquals(first.snapshot(), second.snapshot());
        assertEquals(replacement, first.active());
        assertEquals(2, first.snapshot().size());
        assertEquals(1, before.size());
        assertNotSame(before, first.snapshot());
        assertThrows(UnsupportedOperationException.class, () -> first.snapshot().clear());
    }

    @Test
    void rejectsMismatchedMapKeysWithoutPublishing() {
        RoadThemeRepository repository = new RoadThemeRepository(
                List.of(RoadThemeCatalogue.defaultTheme()));
        List<RoadTheme> before = repository.snapshot();
        RoadTheme custom = RoadThemeJson.parse(
                RoadThemeId.parse("example:actual"),
                RoadThemeJsonTest.validJson());

        assertThrows(IllegalArgumentException.class, () -> repository.replaceCustom(Map.of(
                RoadThemeId.parse("example:wrong"), custom)));
        assertEquals(before, repository.snapshot());
    }

    private static Map<RoadThemeId, RoadTheme> inOrder(RoadTheme... themes) {
        Map<RoadThemeId, RoadTheme> result = new LinkedHashMap<>();
        for (RoadTheme theme : themes) {
            result.put(theme.id(), theme);
        }
        return result;
    }
}
