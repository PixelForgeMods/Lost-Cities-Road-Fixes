package net.austizz.lostcitiesroadfixes.interchange.layout;

public enum ApproachDirection {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    public ApproachDirection opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
        };
    }

    public ApproachDirection rightTurnDestination() {
        return switch (this) {
            case NORTH -> WEST;
            case EAST -> NORTH;
            case SOUTH -> EAST;
            case WEST -> SOUTH;
        };
    }

    public ApproachDirection leftTurnDestination() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }
}
