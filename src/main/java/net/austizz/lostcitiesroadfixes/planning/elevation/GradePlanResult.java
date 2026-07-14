package net.austizz.lostcitiesroadfixes.planning.elevation;

import java.util.Objects;
import java.util.Optional;

public record GradePlanResult(
        int minimumRequiredRunBlocks,
        int requestedRunBlocks,
        Optional<ElevationProfile> profile,
        String diagnostic) {

    public GradePlanResult {
        if (minimumRequiredRunBlocks < 0 || requestedRunBlocks < 0) {
            throw new IllegalArgumentException("Grade plan run lengths cannot be negative");
        }
        profile = Objects.requireNonNull(profile, "profile");
        diagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
    }

    public boolean feasible() {
        return profile.isPresent();
    }

    public ElevationProfile requireProfile() {
        return profile.orElseThrow(() -> new IllegalStateException(diagnostic));
    }
}
