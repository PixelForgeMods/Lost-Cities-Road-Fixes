package net.austizz.lostcitiesroadfixes.theme;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class RoadThemeRepository {
    private static final Comparator<RoadTheme> ORDER = Comparator.comparing(RoadTheme::id);

    private final Map<RoadThemeId, RoadTheme> builtIns;
    private final AtomicReference<List<RoadTheme>> snapshot;

    public RoadThemeRepository(List<RoadTheme> builtIns) {
        this.builtIns = index(Objects.requireNonNull(builtIns, "builtIns"));
        if (!this.builtIns.containsKey(RoadThemeCatalogue.DEFAULT_ID)) {
            throw new IllegalArgumentException("Road themes require a built-in default");
        }
        this.snapshot = new AtomicReference<>(ordered(this.builtIns.values()));
    }

    public List<RoadTheme> snapshot() {
        return snapshot.get();
    }

    public RoadTheme active() {
        return snapshot.get().stream()
                .filter(theme -> theme.id().equals(RoadThemeCatalogue.DEFAULT_ID))
                .findFirst()
                .orElseThrow();
    }

    public void replaceCustom(Map<RoadThemeId, RoadTheme> custom) {
        publish(mergedWith(custom));
    }

    public List<RoadTheme> mergedWith(Map<RoadThemeId, RoadTheme> custom) {
        Objects.requireNonNull(custom, "custom");
        Map<RoadThemeId, RoadTheme> next = new HashMap<>(builtIns);
        custom.forEach((id, theme) -> {
            Objects.requireNonNull(id, "custom theme ID");
            Objects.requireNonNull(theme, "custom theme");
            if (!id.equals(theme.id())) {
                throw new IllegalArgumentException(
                        "Custom theme map key " + id + " does not match theme ID " + theme.id());
            }
            next.put(id, theme);
        });
        return ordered(next.values());
    }

    void publish(List<RoadTheme> next) {
        snapshot.set(List.copyOf(Objects.requireNonNull(next, "next")));
    }

    private static Map<RoadThemeId, RoadTheme> index(List<RoadTheme> themes) {
        Map<RoadThemeId, RoadTheme> result = new HashMap<>();
        for (RoadTheme theme : themes) {
            RoadTheme previous = result.put(theme.id(), theme);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate road theme " + theme.id());
            }
        }
        return Map.copyOf(result);
    }

    private static List<RoadTheme> ordered(java.util.Collection<RoadTheme> themes) {
        return themes.stream().sorted(ORDER).toList();
    }
}
