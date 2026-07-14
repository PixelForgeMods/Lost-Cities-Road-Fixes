package net.austizz.lostcitiesroadfixes.mixin;

import mcjty.lostcities.worldgen.gen.Highways;
import net.austizz.lostcitiesroadfixes.integration.RoadGenerationRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Highways.class, remap = false)
public abstract class HighwaysMixin {
    @Inject(method = "generateHighways", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lostCitiesRoadFixes$suppressNativeHighway(
            CallbackInfo callbackInfo) {
        RoadGenerationRuntime.suppressNativeHighway();
        callbackInfo.cancel();
    }
}
