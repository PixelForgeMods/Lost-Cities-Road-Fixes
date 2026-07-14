package net.austizz.lostcitiesroadfixes.road;

public record HalfBlockElevation(int halfBlocks) implements Comparable<HalfBlockElevation> {
    public static HalfBlockElevation ofWholeBlocks(int blocks) {
        return new HalfBlockElevation(Math.multiplyExact(blocks, 2));
    }

    public double blocks() {
        return halfBlocks / 2.0;
    }

    public int floorBlockY() {
        return Math.floorDiv(halfBlocks, 2);
    }

    public HalfBlockElevation plusHalfBlocks(int amount) {
        return new HalfBlockElevation(Math.addExact(halfBlocks, amount));
    }

    @Override
    public int compareTo(HalfBlockElevation other) {
        return Integer.compare(halfBlocks, other.halfBlocks);
    }
}
