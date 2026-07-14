package net.austizz.lostcitiesroadfixes.interchange.geometry;

import java.util.Objects;

public record RampPose(PlanarPoint point, double headingRadians) {
    public RampPose {
        Objects.requireNonNull(point, "point");
        if (!Double.isFinite(headingRadians)) {
            throw new IllegalArgumentException("Ramp heading must be finite");
        }
        headingRadians = normalize(headingRadians);
    }

    private static double normalize(double angle) {
        double normalized = angle % (StrictMath.PI * 2.0);
        if (normalized >= StrictMath.PI) {
            normalized -= StrictMath.PI * 2.0;
        } else if (normalized < -StrictMath.PI) {
            normalized += StrictMath.PI * 2.0;
        }
        return normalized == -0.0 ? 0.0 : normalized;
    }
}
