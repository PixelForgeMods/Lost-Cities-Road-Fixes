package net.austizz.lostcitiesroadfixes.mixin;

import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.austizz.lostcitiesroadfixes.integration.RoadGenerationRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BuildingInfo.class, remap = false)
public abstract class BuildingInfoMixin {
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void lostCitiesRoadFixes$reserveRoadFootprint(
            ChunkCoord coordinate,
            IDimensionInfo provider,
            CallbackInfo callbackInfo) {
        BuildingInfo building = (BuildingInfo) (Object) this;
        if (building.hasBuilding && RoadGenerationRuntime.shouldSuppressBuilding(
                coordinate,
                provider,
                building.profile,
                building.multiBuildingPos)) {
            building.hasBuilding = false;
        }
    }
}
