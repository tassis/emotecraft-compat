package dev.tassis.emotecraftcompat.mixin;

import dev.tassis.emotecraftcompat.compat.PlayerAnimatorBendCompat;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.kosmx.playerAnim.impl.animation.AnimationApplier", remap = false)
public abstract class PlayerAnimatorAnimationApplierMixin {
    @Inject(
            method = "updatePart(Ljava/lang/String;Lnet/minecraft/client/model/geom/ModelPart;)V",
            at = @At("HEAD"),
            require = 0,
            remap = false
    )
    private void emotecraftCompat$repairBendMutator(
            String partName,
            ModelPart modelPart,
            CallbackInfo callback
    ) {
        PlayerAnimatorBendCompat.ensureBendMutator(partName, modelPart);
    }
}
