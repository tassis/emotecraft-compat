package dev.tassis.emotecraftcompat.mixin;

import dev.tassis.emotecraftcompat.EmotecraftCompatMod;
import io.github.kosmx.bendylib.ModelPartAccessor;
import io.github.kosmx.bendylib.impl.BendableCuboid;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Pseudo
@Mixin(targets = "dev.kosmx.playerAnim.impl.animation.BendHelper", remap = false)
public abstract class PlayerAnimatorBendHelperMixin {
    private static final float BEND_EPSILON = 0.0001F;
    private static final Set<String> WARNED_MODEL_PART_TYPES = ConcurrentHashMap.newKeySet();

    @Inject(
            method = "bend(Lnet/minecraft/client/model/geom/ModelPart;FF)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void emotecraftCompat$applyBendSafely(
            ModelPart modelPart,
            float axis,
            float rotation,
            CallbackInfo callback
    ) {
        if (Math.abs(rotation) < BEND_EPSILON) {
            return;
        }

        boolean bendMutatorUnavailable;
        try {
            bendMutatorUnavailable = ModelPartAccessor.optionalGetCuboid(modelPart, 0)
                    .map(cuboid -> !cuboid.hasMutator("bend")
                            || !(cuboid.getAndActivateMutator("bend") instanceof BendableCuboid))
                    .orElse(false);
        } catch (RuntimeException exception) {
            bendMutatorUnavailable = true;
        }
        if (!bendMutatorUnavailable) {
            return;
        }

        String modelPartType = modelPart.getClass().getName();
        if (WARNED_MODEL_PART_TYPES.add(modelPartType)) {
            EmotecraftCompatMod.LOGGER.warn(
                    "Skipped a Player Animator bend because model part {} has no BendyLib bend mutator",
                    modelPartType
            );
        }
        callback.cancel();
    }
}
