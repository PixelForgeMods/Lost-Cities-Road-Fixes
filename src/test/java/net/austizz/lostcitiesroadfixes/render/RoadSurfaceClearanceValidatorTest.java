package net.austizz.lostcitiesroadfixes.render;

import net.austizz.lostcitiesroadfixes.road.ChunkPoint;
import net.austizz.lostcitiesroadfixes.road.HalfBlockElevation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadSurfaceClearanceValidatorTest {
    private final RoadSurfaceClearanceValidator validator =
            new RoadSurfaceClearanceValidator(7);

    @Test
    void acceptsDecksAtExactlyTheMinimumSeparation() {
        ChunkRoadSurface surface = surfaceAt(140, 154);

        assertDoesNotThrow(() -> validator.requireSafe(surface));
    }

    @Test
    void rejectsDifferentDecksInsideTheVehicleEnvelopeWithCoordinates() {
        ChunkRoadSurface surface = surfaceAt(140, 152);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> validator.requireSafe(surface));

        assertTrue(error.getMessage().contains("4,6"), error::getMessage);
        assertTrue(error.getMessage().contains("140"), error::getMessage);
        assertTrue(error.getMessage().contains("152"), error::getMessage);
    }

    private static ChunkRoadSurface surfaceAt(int lower, int upper) {
        ChunkPoint chunk = new ChunkPoint(0, 0);
        return new ChunkRoadSurface(chunk, List.of(
                cell(4, 6, lower),
                cell(4, 6, upper)));
    }

    private static RoadSurfaceCell cell(int x, int z, int halfBlocks) {
        return new RoadSurfaceCell(
                new RoadSurfacePosition(x, z, new HalfBlockElevation(halfBlocks)),
                RoadSurfaceRole.ASPHALT);
    }
}
