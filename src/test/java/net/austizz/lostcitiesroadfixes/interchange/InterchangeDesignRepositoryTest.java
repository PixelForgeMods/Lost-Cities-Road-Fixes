package net.austizz.lostcitiesroadfixes.interchange;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterchangeDesignRepositoryTest {
    @Test
    void beginsWithAllBuiltInsAndAtomicallyReplacesCustomDesigns() {
        InterchangeDesignRepository repository = new InterchangeDesignRepository(
                InterchangeCatalogue.builtIns());
        List<InterchangeDesign> before = repository.snapshot();
        InterchangeDesign custom = custom(InterchangeDesignId.parse("example:compact"), 61);

        repository.replaceCustom(Map.of(custom.id(), custom));
        List<InterchangeDesign> after = repository.snapshot();

        assertEquals(8, before.size());
        assertEquals(9, after.size());
        assertEquals(8, before.size(), "an existing snapshot must not mutate during reload");
        assertNotSame(before, after);
        assertThrows(UnsupportedOperationException.class, () -> after.clear());
    }

    @Test
    void datapacksCanReplaceAnIdAndInsertionOrderCannotAffectSnapshotOrder() {
        InterchangeDesignId diamondId = InterchangeDesignId.parse("lostcitiesroadfixes:diamond");
        InterchangeDesign replacement = custom(diamondId, 63);
        InterchangeDesign alpha = custom(InterchangeDesignId.parse("example:alpha"), 60);
        InterchangeDesign omega = custom(InterchangeDesignId.parse("example:omega"), 62);

        InterchangeDesignRepository first = new InterchangeDesignRepository(InterchangeCatalogue.builtIns());
        first.replaceCustom(inOrder(replacement, omega, alpha));
        InterchangeDesignRepository second = new InterchangeDesignRepository(InterchangeCatalogue.builtIns());
        second.replaceCustom(inOrder(alpha, replacement, omega));

        assertEquals(first.snapshot(), second.snapshot());
        assertEquals(10, first.snapshot().size());
        assertEquals(63, first.snapshot().stream()
                .filter(design -> design.id().equals(diamondId))
                .findFirst()
                .orElseThrow()
                .minimumRadiusBlocks());
    }

    @Test
    void rejectsMapKeysThatDoNotMatchTheDesignIdWithoutPublishingAnything() {
        InterchangeDesignRepository repository = new InterchangeDesignRepository(
                InterchangeCatalogue.builtIns());
        List<InterchangeDesign> before = repository.snapshot();
        InterchangeDesign custom = custom(InterchangeDesignId.parse("example:actual"), 61);

        assertThrows(IllegalArgumentException.class, () -> repository.replaceCustom(
                Map.of(InterchangeDesignId.parse("example:wrong"), custom)));
        assertEquals(before, repository.snapshot());
    }

    private static Map<InterchangeDesignId, InterchangeDesign> inOrder(InterchangeDesign... designs) {
        Map<InterchangeDesignId, InterchangeDesign> result = new LinkedHashMap<>();
        for (InterchangeDesign design : designs) {
            result.put(design.id(), design);
        }
        return result;
    }

    private static InterchangeDesign custom(InterchangeDesignId id, int radius) {
        return new InterchangeDesign(
                id,
                InterchangeType.DIAMOND,
                JunctionForm.FOUR_WAY,
                radius,
                2,
                96,
                2,
                false,
                false,
                TrafficDemand.REGIONAL,
                2,
                1);
    }
}
