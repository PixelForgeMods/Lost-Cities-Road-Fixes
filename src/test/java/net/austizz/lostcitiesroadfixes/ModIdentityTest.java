package net.austizz.lostcitiesroadfixes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModIdentityTest {
    @Test
    void exposesThePublishedModId() {
        assertEquals("lostcitiesroadfixes", LostCitiesRoadFixes.MOD_ID);
    }
}
