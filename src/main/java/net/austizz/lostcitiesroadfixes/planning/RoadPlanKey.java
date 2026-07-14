package net.austizz.lostcitiesroadfixes.planning;

import net.austizz.lostcitiesroadfixes.road.PlanningRegion;

import java.util.Objects;

public record RoadPlanKey(
        long worldSeed,
        String dimensionId,
        PlanningRegion region,
        String rulesFingerprint) {

    public RoadPlanKey {
        dimensionId = requireText(dimensionId, "dimensionId");
        region = Objects.requireNonNull(region, "region");
        rulesFingerprint = requireText(rulesFingerprint, "rulesFingerprint");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }
}
