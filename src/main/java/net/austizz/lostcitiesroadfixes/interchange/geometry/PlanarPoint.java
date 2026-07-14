package net.austizz.lostcitiesroadfixes.interchange.geometry;

public record PlanarPoint(double x, double z) {
    public PlanarPoint {
        if (!Double.isFinite(x) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Planar coordinates must be finite");
        }
    }
}
