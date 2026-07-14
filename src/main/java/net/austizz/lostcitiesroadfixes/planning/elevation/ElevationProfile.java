package net.austizz.lostcitiesroadfixes.planning.elevation;

import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record ElevationProfile(List<HalfBlockElevation> samples) {
    public ElevationProfile {
        samples = List.copyOf(Objects.requireNonNull(samples, "samples"));
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("An elevation profile needs at least one sample");
        }
        for (int index = 1; index < samples.size(); index++) {
            int change = Math.abs(samples.get(index).halfBlocks() - samples.get(index - 1).halfBlocks());
            if (change > 1) {
                throw new IllegalArgumentException(
                        "Elevation profile changes by more than one half-block at distance " + index);
            }
        }
    }

    public int runBlocks() {
        return samples.size() - 1;
    }

    public HalfBlockElevation elevationAt(int distanceBlocks) {
        if (distanceBlocks < 0 || distanceBlocks >= samples.size()) {
            throw new IndexOutOfBoundsException("Distance is outside elevation profile: " + distanceBlocks);
        }
        return samples.get(distanceBlocks);
    }

    public List<Integer> halfStepPositions() {
        List<Integer> positions = new ArrayList<>();
        for (int index = 1; index < samples.size(); index++) {
            if (!samples.get(index).equals(samples.get(index - 1))) {
                positions.add(index);
            }
        }
        return List.copyOf(positions);
    }
}
