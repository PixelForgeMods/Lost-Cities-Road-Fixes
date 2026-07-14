package net.austizz.lostcitiesroadfixes.planning;

import java.nio.charset.StandardCharsets;

public final class PlanningSeed {
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final long REGION_X_SALT = 0x632be59bd9b4e019L;
    private static final long REGION_Z_SALT = 0x9e3779b97f4a7c15L;

    private PlanningSeed() {
    }

    public static long derive(RoadPlanKey key) {
        long state = mix64(key.worldSeed());
        state = mix64(state ^ hashText(key.dimensionId()));
        state = mix64(state ^ (key.region().x() * REGION_X_SALT));
        state = mix64(state ^ (key.region().z() * REGION_Z_SALT));
        return mix64(state ^ hashText(key.rulesFingerprint()));
    }

    private static long hashText(String value) {
        long hash = FNV_OFFSET_BASIS;
        for (byte element : value.getBytes(StandardCharsets.UTF_8)) {
            hash ^= Byte.toUnsignedLong(element);
            hash *= FNV_PRIME;
        }
        return hash;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
