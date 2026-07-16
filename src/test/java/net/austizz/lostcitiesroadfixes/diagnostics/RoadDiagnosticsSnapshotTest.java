package net.austizz.lostcitiesroadfixes.diagnostics;

import net.austizz.lostcitiesroadfixes.interchange.InterchangeType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadDiagnosticsSnapshotTest {
    @Test
    void rendersStableCompleteOperatorLines() {
        RoadDiagnosticsSnapshot snapshot = new RoadDiagnosticsSnapshot(
                true,
                12,
                34,
                5,
                6,
                7,
                Map.of(
                        InterchangeType.DIAMOND, 5L,
                        InterchangeType.CLOVERLEAF, 2L),
                8,
                9,
                10,
                11,
                12,
                13,
                14,
                15,
                "example:requested",
                "lostcitiesroadfixes:default",
                4,
                512,
                false);

        List<String> lines = snapshot.lines();

        assertEquals("Lost Cities: Road Fixes diagnostics", lines.getFirst());
        assertTrue(lines.stream().anyMatch(line -> line.contains("compatible=true")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("buildingChunkSuppressions=34")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("selected=7")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("diamond=5")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("cloverleaf=2")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("conflicted=9")));
        assertTrue(lines.stream().anyMatch(line ->
                line.contains("straightThroughFallbacks=11")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("roads=12")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("loadedDesigns=14")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("example:requested")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("fallback=true")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("maximumGapChunks=4")));
        assertEquals(String.join(" | ", lines), snapshot.compactLine());
        assertThrows(UnsupportedOperationException.class, () -> lines.clear());
    }

    @Test
    void rejectsNegativeCountersAndBlankThemeIds() {
        assertThrows(IllegalArgumentException.class, () -> new RoadDiagnosticsSnapshot(
                true, -1, 0, 0, 0, 0, Map.of(), 0, 0, 0, 0, 0, 0, 0, 0,
                "a:b", "a:b", 1, 64, true));
        assertThrows(IllegalArgumentException.class, () -> new RoadDiagnosticsSnapshot(
                true, 0, 0, 0, 0, 0, Map.of(), 0, 0, 0, 0, 0, 0, 0, 0,
                " ", "a:b", 1, 64, true));
        assertThrows(IllegalArgumentException.class, () -> new RoadDiagnosticsSnapshot(
                true, 0, 0, 0, 0, 0,
                Map.of(InterchangeType.DIAMOND, -1L),
                0, 0, 0, 0, 0, 0, 0, 0,
                "a:b", "a:b", 1, 64, true));
    }
}
