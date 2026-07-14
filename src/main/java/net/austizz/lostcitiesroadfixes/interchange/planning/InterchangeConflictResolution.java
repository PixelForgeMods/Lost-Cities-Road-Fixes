package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record InterchangeConflictResolution(
        List<PlannedInterchange> interchanges,
        List<ConflictedRoadCrossing> conflicts) {
    public InterchangeConflictResolution {
        Objects.requireNonNull(interchanges, "interchanges");
        Objects.requireNonNull(conflicts, "conflicts");
        interchanges = List.copyOf(interchanges.stream().sorted().toList());
        conflicts = List.copyOf(conflicts.stream().sorted().toList());

        Set<ChunkPoint> chunks = new HashSet<>();
        interchanges.forEach(interchange -> addUnique(chunks, interchange.crossing().chunk()));
        conflicts.forEach(conflict -> addUnique(chunks, conflict.crossing().chunk()));
    }

    private static void addUnique(Set<ChunkPoint> chunks, ChunkPoint chunk) {
        if (!chunks.add(chunk)) {
            throw new IllegalArgumentException("Duplicate conflict candidate " + chunk);
        }
    }
}
