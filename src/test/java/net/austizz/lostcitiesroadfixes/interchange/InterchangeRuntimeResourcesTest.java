package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.interchange.layout.ApproachDirection;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeGeometryBlueprint;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeMovement;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeMovementBlueprint;
import net.austizz.lostcitiesroadfixes.interchange.layout.MovementKind;
import net.austizz.lostcitiesroadfixes.interchange.layout.RampControl;
import net.austizz.lostcitiesroadfixes.interchange.layout.RampForm;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class InterchangeRuntimeResourcesTest {
    @Test
    void fingerprintIsOrderIndependentAndIncludesDesignFields() {
        List<InterchangeDesign> forward = InterchangeCatalogue.builtIns();
        List<InterchangeDesign> reverse = new ArrayList<>(forward);
        Collections.reverse(reverse);
        InterchangeDesign first = forward.getFirst();
        InterchangeDesign changed = new InterchangeDesign(
                first.id(),
                first.type(),
                first.form(),
                first.minimumRadiusBlocks(),
                first.requiredQuadrants(),
                first.minimumApproachRunBlocks(),
                first.structureLevels(),
                first.usesLoopRamps(),
                first.allMovementsFreeFlow(),
                first.capacity(),
                first.freeFlowMovementCount(),
                first.constructionComplexity() + 1);
        List<InterchangeDesign> modified = new ArrayList<>(forward);
        modified.set(0, changed);

        assertEquals(
                InterchangeDesignFingerprint.of(forward),
                InterchangeDesignFingerprint.of(reverse));
        assertNotEquals(
                InterchangeDesignFingerprint.of(forward),
                InterchangeDesignFingerprint.of(modified));
    }

    @Test
    void successfulReloadInstallsAtomicallyThenInvalidatesRuntimePlans() {
        InterchangeDesignRepository repository = new InterchangeDesignRepository(
                InterchangeCatalogue.builtIns());
        AtomicInteger invalidations = new AtomicInteger();
        InterchangeDesignInstaller installer = new InterchangeDesignInstaller(
                repository, invalidations::incrementAndGet);
        InterchangeDesign custom = customDesign();

        installer.install(Map.of(custom.id(), custom));

        assertEquals(1, invalidations.get());
        assertEquals(custom, repository.snapshot().stream()
                .filter(design -> design.id().equals(custom.id()))
                .findFirst()
                .orElseThrow());
    }

    @Test
    void fingerprintChangesWhenOnlyCustomMovementGeometryChanges() {
        InterchangeDesign narrow = customDesignWithGeometry(8);
        InterchangeDesign wide = customDesignWithGeometry(9);

        assertNotEquals(
                InterchangeDesignFingerprint.of(List.of(narrow)),
                InterchangeDesignFingerprint.of(List.of(wide)));
    }

    @Test
    void movementArrayOrderCannotChangeTheBlueprintOrFingerprint() {
        InterchangeDesign design = customDesignWithGeometry(8);
        InterchangeGeometryBlueprint forward = design.geometry().orElseThrow();
        List<InterchangeMovementBlueprint> reversed = new ArrayList<>(forward.movements());
        Collections.reverse(reversed);
        InterchangeGeometryBlueprint reverse = new InterchangeGeometryBlueprint(reversed);

        assertEquals(forward, reverse);
        InterchangeDesign reordered = new InterchangeDesign(
                design.id(),
                design.type(),
                design.form(),
                design.minimumRadiusBlocks(),
                design.requiredQuadrants(),
                design.minimumApproachRunBlocks(),
                design.structureLevels(),
                design.usesLoopRamps(),
                design.allMovementsFreeFlow(),
                design.capacity(),
                design.freeFlowMovementCount(),
                design.constructionComplexity(),
                Optional.of(reverse));
        assertEquals(
                InterchangeDesignFingerprint.of(List.of(design)),
                InterchangeDesignFingerprint.of(List.of(reordered)));
    }

    private static InterchangeDesign customDesign() {
        return new InterchangeDesign(
                InterchangeDesignId.parse("example:runtime_diamond"),
                InterchangeType.DIAMOND,
                JunctionForm.FOUR_WAY,
                40,
                2,
                128,
                2,
                false,
                false,
                TrafficDemand.REGIONAL,
                4,
                5);
    }

    private static InterchangeDesign customDesignWithGeometry(int firstWidth) {
        List<InterchangeMovementBlueprint> movements = new ArrayList<>();
        for (ApproachDirection from : ApproachDirection.values()) {
            movements.add(new InterchangeMovementBlueprint(
                    new InterchangeMovement(
                            from, from.opposite(), MovementKind.STRAIGHT),
                    RampForm.MAINLINE,
                    RampControl.FREE_FLOW,
                    8,
                    1));
            movements.add(new InterchangeMovementBlueprint(
                    new InterchangeMovement(
                            from, from.rightTurnDestination(), MovementKind.RIGHT),
                    RampForm.DIRECT,
                    RampControl.YIELD,
                    from == ApproachDirection.NORTH ? firstWidth : 8,
                    1));
            movements.add(new InterchangeMovementBlueprint(
                    new InterchangeMovement(
                            from, from.leftTurnDestination(), MovementKind.LEFT),
                    RampForm.DIRECT,
                    RampControl.SIGNALIZED,
                    8,
                    2));
        }
        return new InterchangeDesign(
                InterchangeDesignId.parse("example:fingerprint"),
                InterchangeType.DIAMOND,
                JunctionForm.FOUR_WAY,
                56,
                2,
                96,
                2,
                false,
                false,
                TrafficDemand.REGIONAL,
                4,
                1,
                Optional.of(new InterchangeGeometryBlueprint(movements)));
    }
}
