package net.austizz.lostcitiesroadfixes.regression;

public record MiniExplosionSettings(
        float chance,
        int minimumRadius,
        int maximumRadiusExclusive,
        int minimumHeight,
        int maximumHeightExclusive) {

    public static final MiniExplosionSettings LOST_CITIES_DEFAULTS =
            new MiniExplosionSettings(0.03f, 5, 12, 60, 100);

    public MiniExplosionSettings {
        if (chance < 0.0f || chance > 1.0f) {
            throw new IllegalArgumentException("Mini-explosion chance must be between 0 and 1");
        }
        if (minimumRadius < 1 || maximumRadiusExclusive <= minimumRadius) {
            throw new IllegalArgumentException("Mini-explosion radius range must be non-empty and positive");
        }
        if (maximumHeightExclusive <= minimumHeight) {
            throw new IllegalArgumentException("Mini-explosion height range must be non-empty");
        }
    }
}
