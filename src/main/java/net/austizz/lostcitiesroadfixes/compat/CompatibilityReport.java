package net.austizz.lostcitiesroadfixes.compat;

import java.util.List;

public record CompatibilityReport(
        boolean compatible,
        String implementationVersion,
        String sha256,
        List<String> verifiedSymbols,
        List<String> errors) {

    public CompatibilityReport {
        verifiedSymbols = List.copyOf(verifiedSymbols);
        errors = List.copyOf(errors);
    }

    public String diagnostic() {
        if (compatible) {
            return "Lost Cities " + implementationVersion + " (" + sha256 + ") is compatible";
        }
        return "Unsupported Lost Cities binary: " + String.join("; ", errors);
    }
}
