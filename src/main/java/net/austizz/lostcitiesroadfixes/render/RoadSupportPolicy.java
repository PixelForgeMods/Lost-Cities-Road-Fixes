package net.austizz.lostcitiesroadfixes.render;

public record RoadSupportPolicy(boolean enabled, int maximumDepthBlocks) {
    private static final int MAXIMUM_ALLOWED_DEPTH = 256;

    public RoadSupportPolicy {
        if (enabled && (maximumDepthBlocks < 1
                || maximumDepthBlocks > MAXIMUM_ALLOWED_DEPTH)) {
            throw new IllegalArgumentException(
                    "Enabled road support depth must be between 1 and "
                            + MAXIMUM_ALLOWED_DEPTH + " blocks");
        }
        if (!enabled && maximumDepthBlocks != 0) {
            throw new IllegalArgumentException("Disabled road supports must have zero depth");
        }
    }

    public static RoadSupportPolicy enabled(int maximumDepthBlocks) {
        return new RoadSupportPolicy(true, maximumDepthBlocks);
    }

    public static RoadSupportPolicy disabled() {
        return new RoadSupportPolicy(false, 0);
    }
}
