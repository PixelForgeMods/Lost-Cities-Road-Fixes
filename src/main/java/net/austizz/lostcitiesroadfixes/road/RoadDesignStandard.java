package net.austizz.lostcitiesroadfixes.road;

import java.util.Objects;

public final class RoadDesignStandard {
    public static final RoadDesignStandard DEFAULT = new RoadDesignStandard(
            new RoadCrossSection(2, 7, 2, 1),
            1,
            8,
            32,
            24,
            7,
            10);

    private final RoadCrossSection arterialCrossSection;
    private final int maximumRiseHalfBlocks;
    private final int gradeRunBlocks;
    private final int minimumArterialRadiusBlocks;
    private final int minimumRampRadiusBlocks;
    private final int minimumVehicleClearanceBlocks;
    private final int preferredDeckSeparationBlocks;

    public RoadDesignStandard(
            RoadCrossSection arterialCrossSection,
            int maximumRiseHalfBlocks,
            int gradeRunBlocks,
            int minimumArterialRadiusBlocks,
            int minimumRampRadiusBlocks,
            int minimumVehicleClearanceBlocks,
            int preferredDeckSeparationBlocks) {
        this.arterialCrossSection = Objects.requireNonNull(arterialCrossSection, "arterialCrossSection");
        if (arterialCrossSection.totalWidthBlocks() != 32) {
            throw new IllegalArgumentException("The arterial cross-section must be exactly 32 blocks wide");
        }
        if (maximumRiseHalfBlocks < 1 || gradeRunBlocks < 1) {
            throw new IllegalArgumentException("Grade limits must be positive");
        }
        if (minimumArterialRadiusBlocks < 1 || minimumRampRadiusBlocks < 1) {
            throw new IllegalArgumentException("Curve radii must be positive");
        }
        if (minimumVehicleClearanceBlocks < 1
                || preferredDeckSeparationBlocks < minimumVehicleClearanceBlocks) {
            throw new IllegalArgumentException(
                    "Deck separation must be at least the minimum vehicle clearance");
        }
        this.maximumRiseHalfBlocks = maximumRiseHalfBlocks;
        this.gradeRunBlocks = gradeRunBlocks;
        this.minimumArterialRadiusBlocks = minimumArterialRadiusBlocks;
        this.minimumRampRadiusBlocks = minimumRampRadiusBlocks;
        this.minimumVehicleClearanceBlocks = minimumVehicleClearanceBlocks;
        this.preferredDeckSeparationBlocks = preferredDeckSeparationBlocks;
    }

    public RoadCrossSection arterialCrossSection() {
        return arterialCrossSection;
    }

    public boolean acceptsGrade(RoadGrade grade) {
        Objects.requireNonNull(grade, "grade");
        return (long) grade.riseHalfBlocks() * gradeRunBlocks
                <= (long) grade.horizontalRunBlocks() * maximumRiseHalfBlocks;
    }

    public int maximumRiseHalfBlocks() {
        return maximumRiseHalfBlocks;
    }

    public int gradeRunBlocks() {
        return gradeRunBlocks;
    }

    public void requireGrade(RoadGrade grade) {
        if (!acceptsGrade(grade)) {
            throw new IllegalArgumentException(
                    "Road grade exceeds " + maximumRiseHalfBlocks
                            + " half-block rise per " + gradeRunBlocks + " horizontal blocks");
        }
    }

    public int minimumCurveRadiusBlocks(RoadKind kind) {
        return switch (Objects.requireNonNull(kind, "kind")) {
            case ARTERIAL -> minimumArterialRadiusBlocks;
            case RAMP -> minimumRampRadiusBlocks;
        };
    }

    public void requireCurveRadius(RoadKind kind, int radiusBlocks) {
        int minimum = minimumCurveRadiusBlocks(kind);
        if (radiusBlocks < minimum) {
            throw new IllegalArgumentException(
                    kind + " curve radius must be at least " + minimum + " blocks");
        }
    }

    public void requireVehicleClearance(int clearanceBlocks) {
        if (clearanceBlocks < minimumVehicleClearanceBlocks) {
            throw new IllegalArgumentException(
                    "Vehicle clearance must be at least " + minimumVehicleClearanceBlocks + " blocks");
        }
    }

    public int minimumVehicleClearanceBlocks() {
        return minimumVehicleClearanceBlocks;
    }

    public int preferredDeckSeparationBlocks() {
        return preferredDeckSeparationBlocks;
    }
}
