package net.austizz.lostcitiesroadfixes.interchange;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
}
