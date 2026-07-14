package net.austizz.lostcitiesroadfixes.interchange.planning;

import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;

import java.util.Objects;

public final class CrossingElevationModel {
    private final HalfBlockElevation groundLevel;
    private final int levelSpacingBlocks;
    private final RoadDesignStandard standard;

    public CrossingElevationModel(
            HalfBlockElevation groundLevel,
            int levelSpacingBlocks,
            RoadDesignStandard standard) {
        this.groundLevel = Objects.requireNonNull(groundLevel, "groundLevel");
        this.standard = Objects.requireNonNull(standard, "standard");
        if (levelSpacingBlocks < 1) {
            throw new IllegalArgumentException("Road level spacing must be positive");
        }
        this.levelSpacingBlocks = levelSpacingBlocks;
    }

    public HalfBlockElevation nativeElevation(int level) {
        if (level < 0) {
            throw new IllegalArgumentException("Road level cannot be negative");
        }
        return groundLevel.plusHalfBlocks(Math.multiplyExact(
                level, Math.multiplyExact(levelSpacingBlocks, 2)));
    }

    public CrossingDecks planDecks(int xLevel, int zLevel) {
        HalfBlockElevation nativeX = nativeElevation(xLevel);
        HalfBlockElevation nativeZ = nativeElevation(zLevel);
        if (nativeX.equals(nativeZ)) {
            throw new IllegalArgumentException("Cannot separate equal-level roads as an interchange");
        }

        HalfBlockElevation lower = nativeX.compareTo(nativeZ) < 0 ? nativeX : nativeZ;
        HalfBlockElevation nativeUpper = nativeX.compareTo(nativeZ) > 0 ? nativeX : nativeZ;
        HalfBlockElevation preferredUpper = lower.plusHalfBlocks(
                Math.multiplyExact(standard.preferredDeckSeparationBlocks(), 2));
        HalfBlockElevation plannedUpper = nativeUpper.compareTo(preferredUpper) >= 0
                ? nativeUpper
                : preferredUpper;
        return nativeX.compareTo(nativeZ) < 0
                ? new CrossingDecks(nativeX, nativeZ, lower, plannedUpper)
                : new CrossingDecks(nativeX, nativeZ, plannedUpper, lower);
    }
}
