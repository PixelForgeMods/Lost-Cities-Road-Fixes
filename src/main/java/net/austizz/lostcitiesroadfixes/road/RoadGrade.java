package net.austizz.lostcitiesroadfixes.road;

public record RoadGrade(int riseHalfBlocks, int horizontalRunBlocks) {
    public RoadGrade {
        if (riseHalfBlocks < 0) {
            throw new IllegalArgumentException("Grade rise cannot be negative");
        }
        if (horizontalRunBlocks < 1) {
            throw new IllegalArgumentException("Grade run must be positive");
        }
    }
}
