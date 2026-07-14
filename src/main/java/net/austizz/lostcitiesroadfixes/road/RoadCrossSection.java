package net.austizz.lostcitiesroadfixes.road;

public record RoadCrossSection(
        int lanesPerDirection,
        int laneWidthBlocks,
        int medianWidthBlocks,
        int shoulderWidthBlocks) {

    public RoadCrossSection {
        if (lanesPerDirection < 1) {
            throw new IllegalArgumentException("A road needs at least one lane per direction");
        }
        if (laneWidthBlocks < 1) {
            throw new IllegalArgumentException("Lane width must be positive");
        }
        if (medianWidthBlocks < 0 || shoulderWidthBlocks < 0) {
            throw new IllegalArgumentException("Median and shoulder widths cannot be negative");
        }
    }

    public int totalWidthBlocks() {
        return Math.addExact(
                Math.addExact(
                        Math.multiplyExact(Math.multiplyExact(2, lanesPerDirection), laneWidthBlocks),
                        medianWidthBlocks),
                Math.multiplyExact(2, shoulderWidthBlocks));
    }
}
