package net.austizz.lostcitiesroadfixes.interchange;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class InterchangeDesignFingerprint {
    private static final Comparator<InterchangeDesign> ORDER = Comparator
            .comparingInt((InterchangeDesign design) -> design.type().ordinal())
            .thenComparing(InterchangeDesign::id);

    private InterchangeDesignFingerprint() {
    }

    public static String of(List<InterchangeDesign> designs) {
        Objects.requireNonNull(designs, "designs");
        List<InterchangeDesign> ordered = designs.stream().sorted(ORDER).toList();
        return "interchange-designs-v2|" + ordered;
    }
}
