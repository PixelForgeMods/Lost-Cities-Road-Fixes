package net.austizz.lostcitiesroadfixes.render;

import java.util.Comparator;
import java.util.Objects;

public record RoadSurfaceCell(RoadSurfacePosition position, RoadSurfaceRole role) {
    public static final Comparator<RoadSurfaceCell> ORDER = Comparator.comparing(RoadSurfaceCell::position);

    public RoadSurfaceCell {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(role, "role");
    }
}
