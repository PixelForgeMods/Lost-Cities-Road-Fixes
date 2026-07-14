package net.austizz.lostcitiesroadfixes.diagnostics;

import org.junit.jupiter.api.Test;

import java.util.List;

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
                8,
                9,
                10,
                11,
                12,
                "example:requested",
                "lostcitiesroadfixes:default",
                4,
                512,
                false);

        List<String> lines = snapshot.lines();

        assertEquals("Lost Cities: Road Fixes diagnostics", lines.getFirst());
        assertTrue(lines.stream().anyMatch(line -> line.contains("compatible=true")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("selected=6")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("roads=9")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("loadedDesigns=11")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("example:requested")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("fallback=true")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("maximumGapChunks=4")));
        assertThrows(UnsupportedOperationException.class, () -> lines.clear());
    }

    @Test
    void rejectsNegativeCountersAndBlankThemeIds() {
        assertThrows(IllegalArgumentException.class, () -> new RoadDiagnosticsSnapshot(
                true, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                "a:b", "a:b", 1, 64, true));
        assertThrows(IllegalArgumentException.class, () -> new RoadDiagnosticsSnapshot(
                true, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                " ", "a:b", 1, 64, true));
    }
}
