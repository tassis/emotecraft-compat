package dev.tassis.emotecraftcompat.mixin;

import dev.tassis.emotecraftcompat.compat.EmfCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "traben.entity_model_features.models.animation.EMFAnimationEntityContext", remap = false)
public abstract class EMFAnimationEntityContextMixin {
    @Inject(method = "isEntityAnimPaused", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void emotecraftCompat$pauseLegacy(CallbackInfoReturnable<Boolean> callback) {
        pauseIfPlayerIsEmoting(callback);
    }

    @Inject(method = "isEntityAnimPausedWrapped", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void emotecraftCompat$pauseWrapped(CallbackInfoReturnable<Boolean> callback) {
        pauseIfPlayerIsEmoting(callback);
    }

    @Inject(method = "isEntityForcedToVanillaModel", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void emotecraftCompat$forceVanillaModel(CallbackInfoReturnable<Boolean> callback) {
        if (EmfCompat.shouldUseLegacyVanillaFallback() && EmfCompat.shouldSuppressCurrentAnimatedPlayer()) {
            callback.setReturnValue(true);
        }
    }

    private static void pauseIfPlayerIsEmoting(CallbackInfoReturnable<Boolean> callback) {
        if (EmfCompat.shouldUseLegacyPauseFallback() && EmfCompat.shouldSuppressCurrentAnimatedPlayer()) {
            callback.setReturnValue(true);
        }
    }
}
