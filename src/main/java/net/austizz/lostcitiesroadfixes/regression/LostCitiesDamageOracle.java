package net.austizz.lostcitiesroadfixes.regression;

import java.util.Optional;
import java.util.Random;

public final class LostCitiesDamageOracle {
    private static final long MINI_EXPLOSION_Z_SALT = 1_400_305_337L;
    private static final long MINI_EXPLOSION_X_SALT = 573_259_391L;

    private LostCitiesDamageOracle() {
    }

    public static Optional<MiniExplosionSample> sampleMiniExplosion(
            long worldSeed,
            ChunkCoordinate sourceChunk,
            int cityLevel,
            MiniExplosionSettings settings) {
        Random random = new Random(worldSeed
                + sourceChunk.z() * MINI_EXPLOSION_Z_SALT
                + sourceChunk.x() * MINI_EXPLOSION_X_SALT);
        float chanceRoll = random.nextFloat();
        if (chanceRoll >= settings.chance()) {
            return Optional.empty();
        }

        int radius = settings.minimumRadius()
                + random.nextInt(settings.maximumRadiusExclusive() - settings.minimumRadius());
        int x = sourceChunk.minBlockX() + random.nextInt(16);
        int y = cityLevel * 6 + settings.minimumHeight()
                + random.nextInt(settings.maximumHeightExclusive() - settings.minimumHeight());
        int z = sourceChunk.minBlockZ() + random.nextInt(16);
        return Optional.of(new MiniExplosionSample(chanceRoll, radius, new BlockCoordinate(x, y, z)));
    }
}
