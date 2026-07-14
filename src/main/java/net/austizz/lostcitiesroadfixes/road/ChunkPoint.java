package net.austizz.lostcitiesroadfixes.road;

public record ChunkPoint(int x, int z) {
    public int minBlockX() {
        return x << 4;
    }

    public int maxBlockX() {
        return minBlockX() + 15;
    }

    public int minBlockZ() {
        return z << 4;
    }

    public int maxBlockZ() {
        return minBlockZ() + 15;
    }
}
