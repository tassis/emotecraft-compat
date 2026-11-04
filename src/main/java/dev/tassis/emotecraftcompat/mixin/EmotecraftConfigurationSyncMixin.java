package dev.tassis.emotecraftcompat.mixin;

import dev.tassis.emotecraftcompat.compat.EmotecraftServerSyncCompat;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Pseudo
@Mixin(targets = "io.github.kosmx.emotes.neoforge.network.ForgeNetwork", remap = false)
public abstract class EmotecraftConfigurationSyncMixin {
    @Inject(
            method = "lambda$registerPlay$6(Lnet/neoforged/neoforge/network/handling/IPayloadContext;Ljava/nio/ByteBuffer;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private static void emotecraftCompat$sendServerEmote(
            IPayloadContext context,
            ByteBuffer buffer,
            CallbackInfo callback
    ) {
        EmotecraftServerSyncCompat.sendServerEmote(context, buffer);
        callback.cancel();
    }
}
