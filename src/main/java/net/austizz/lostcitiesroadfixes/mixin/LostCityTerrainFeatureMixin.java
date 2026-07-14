package net.austizz.lostcitiesroadfixes.mixin;

import mcjty.lostcities.worldgen.LostCityTerrainFeature;
import net.austizz.lostcitiesroadfixes.integration.RoadGenerationRuntime;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LostCityTerrainFeature.class, remap = false)
public abstract class LostCityTerrainFeatureMixin {
    @Inject(method = "generate", at = @At("RETURN"), remap = false)
    private void lostCitiesRoadFixes$renderAfterCleanup(
            WorldGenRegion region,
            ChunkAccess chunk,
            CallbackInfo callbackInfo) {
        RoadGenerationRuntime.renderAfterLostCities(
                (LostCityTerrainFeature) (Object) this,
                region,
                chunk);
    }
}
