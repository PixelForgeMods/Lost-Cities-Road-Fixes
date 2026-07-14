package net.austizz.lostcitiesroadfixes.interchange.layout;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesign;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record InterchangeLayout(
        InterchangeDesign design,
        InterchangeGeometrySite site,
        Set<ApproachDirection> approaches,
        List<InterchangeConnection> connections) {
    public InterchangeLayout {
        Objects.requireNonNull(design, "design");
        Objects.requireNonNull(site, "site");
        Objects.requireNonNull(approaches, "approaches");
        approaches = Collections.unmodifiableSet(EnumSet.copyOf(approaches));
        connections = List.copyOf(connections);

        Set<InterchangeMovement> movements = new HashSet<>();
        for (InterchangeConnection connection : connections) {
            if (!approaches.contains(connection.movement().from())
                    || !approaches.contains(connection.movement().to())) {
                throw new IllegalArgumentException(
                        "Connection uses an unavailable approach: " + connection.movement());
            }
            if (!movements.add(connection.movement())) {
                throw new IllegalArgumentException("Duplicate movement " + connection.movement());
            }
            if (connection.structureLevel() > design.structureLevels()) {
                throw new IllegalArgumentException(
                        "Connection exceeds design structure levels: " + connection);
            }
        }
    }
}
