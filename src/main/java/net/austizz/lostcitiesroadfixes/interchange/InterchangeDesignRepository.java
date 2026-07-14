package net.austizz.lostcitiesroadfixes.interchange;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class InterchangeDesignRepository {
    private static final Comparator<InterchangeDesign> ORDER = Comparator
            .comparingInt((InterchangeDesign design) -> design.type().ordinal())
            .thenComparing(InterchangeDesign::id);

    private final Map<InterchangeDesignId, InterchangeDesign> builtIns;
    private final AtomicReference<List<InterchangeDesign>> snapshot;

    public InterchangeDesignRepository(List<InterchangeDesign> builtIns) {
        Objects.requireNonNull(builtIns, "builtIns");
        this.builtIns = index(builtIns);
        this.snapshot = new AtomicReference<>(ordered(this.builtIns.values()));
    }

    public List<InterchangeDesign> snapshot() {
        return snapshot.get();
    }

    public void replaceCustom(Map<InterchangeDesignId, InterchangeDesign> custom) {
        Objects.requireNonNull(custom, "custom");
        Map<InterchangeDesignId, InterchangeDesign> next = new HashMap<>(builtIns);
        custom.forEach((id, design) -> {
            Objects.requireNonNull(id, "custom design ID");
            Objects.requireNonNull(design, "custom design");
            if (!id.equals(design.id())) {
                throw new IllegalArgumentException(
                        "Custom design map key " + id + " does not match design ID " + design.id());
            }
            next.put(id, design);
        });
        snapshot.set(ordered(next.values()));
    }

    private static Map<InterchangeDesignId, InterchangeDesign> index(List<InterchangeDesign> designs) {
        Map<InterchangeDesignId, InterchangeDesign> indexed = new HashMap<>();
        for (InterchangeDesign design : designs) {
            InterchangeDesign previous = indexed.put(design.id(), design);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate interchange design " + design.id());
            }
        }
        return Map.copyOf(indexed);
    }

    private static List<InterchangeDesign> ordered(java.util.Collection<InterchangeDesign> designs) {
        return designs.stream().sorted(ORDER).toList();
    }
}
