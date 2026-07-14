package net.austizz.lostcitiesroadfixes.theme;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class RoadThemeResources {
    private static final RoadThemeCompiler COMPILER = new RoadThemeCompiler();
    private static final RoadThemeRepository REPOSITORY =
            new RoadThemeRepository(List.of(RoadThemeCatalogue.defaultTheme()));
    private static final AtomicReference<List<CompiledRoadTheme>> COMPILED =
            new AtomicReference<>(compile(REPOSITORY.snapshot()));

    private RoadThemeResources() {
    }

    public static RoadThemeRepository repository() {
        return REPOSITORY;
    }

    public static CompiledRoadTheme active() {
        return active(RoadThemeCatalogue.DEFAULT_ID);
    }

    public static CompiledRoadTheme active(RoadThemeId requested) {
        List<CompiledRoadTheme> snapshot = COMPILED.get();
        return snapshot.stream()
                .filter(theme -> theme.id().equals(requested))
                .findFirst()
                .orElseGet(() -> snapshot.stream()
                .filter(theme -> theme.id().equals(RoadThemeCatalogue.DEFAULT_ID))
                .findFirst()
                .orElseThrow());
    }

    public static int loadedThemeCount() {
        return COMPILED.get().size();
    }

    public static synchronized void install(Map<RoadThemeId, RoadTheme> custom) {
        List<RoadTheme> next = REPOSITORY.mergedWith(custom);
        List<CompiledRoadTheme> compiled = compile(next);
        REPOSITORY.publish(next);
        COMPILED.set(compiled);
    }

    private static List<CompiledRoadTheme> compile(List<RoadTheme> themes) {
        return themes.stream().map(COMPILER::compile).toList();
    }
}
