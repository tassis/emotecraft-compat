package dev.tassis.emotecraftcompat.mixin;

import dev.tassis.emotecraftcompat.compat.PlayerAnimatorBendCompat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(value = PlayerModel.class, priority = 100)
public abstract class PlayerModelLayerBendMixin<T extends LivingEntity> extends HumanoidModel<T> {
    protected PlayerModelLayerBendMixin(
            ModelPart root,
            Function<ResourceLocation, RenderType> renderType
    ) {
        super(root, renderType);
    }

    @Shadow
    @Final
    public ModelPart jacket;

    @Shadow
    @Final
    public ModelPart rightSleeve;

    @Shadow
    @Final
    public ModelPart leftSleeve;

    @Shadow
    @Final
    public ModelPart rightPants;

    @Shadow
    @Final
    public ModelPart leftPants;

    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At("HEAD")
    )
    private void emotecraftCompat$repairOuterLayerBendMutators(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callback
    ) {
        PlayerAnimatorBendCompat.ensureBendMutator("jacket", jacket);
        PlayerAnimatorBendCompat.ensureBendMutator("rightSleeve", rightSleeve);
        PlayerAnimatorBendCompat.ensureBendMutator("leftSleeve", leftSleeve);
        PlayerAnimatorBendCompat.ensureBendMutator("rightPants", rightPants);
        PlayerAnimatorBendCompat.ensureBendMutator("leftPants", leftPants);
    }
}
