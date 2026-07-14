package net.austizz.lostcitiesroadfixes.interchange.geometry;

public enum RoadHeading {
    EAST(0.0),
    SOUTH(StrictMath.PI / 2.0),
    WEST(StrictMath.PI),
    NORTH(-StrictMath.PI / 2.0);

    private final double radians;

    RoadHeading(double radians) {
        this.radians = radians;
    }

    public double radians() {
        return radians;
    }
}
