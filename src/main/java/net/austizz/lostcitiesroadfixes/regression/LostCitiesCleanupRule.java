package net.austizz.lostcitiesroadfixes.regression;

public final class LostCitiesCleanupRule {
    public static final int MINIMUM_STABLE_LAYER_BLOCKS = 16;

    private LostCitiesCleanupRule() {
    }

    public static boolean deletesBlocksAbove(int nonAirBlocks) {
        if (nonAirBlocks < 0 || nonAirBlocks > 256) {
            throw new IllegalArgumentException("A chunk layer must contain between 0 and 256 blocks");
        }
        return nonAirBlocks < MINIMUM_STABLE_LAYER_BLOCKS;
    }
}
