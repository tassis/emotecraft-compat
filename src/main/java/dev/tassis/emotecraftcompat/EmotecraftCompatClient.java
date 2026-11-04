package dev.tassis.emotecraftcompat;

import dev.tassis.emotecraftcompat.compat.EmfCompat;
import dev.tassis.emotecraftcompat.compat.PlayerAnimatorBendCompat;
import dev.tassis.emotecraftcompat.compat.SkinLayersBendCompat;
import dev.tassis.emotecraftcompat.state.EmoteStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

@EventBusSubscriber(
        modid = EmotecraftCompatMod.MOD_ID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class EmotecraftCompatClient {
    private EmotecraftCompatClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EmotecraftCompatMod.LOGGER.info("Detected Emotecraft loaded: {}", ModList.get().isLoaded("emotecraft"));
        EmotecraftCompatMod.LOGGER.info("Detected EMF loaded: {}", ModList.get().isLoaded("entity_model_features"));
        EmotecraftCompatMod.LOGGER.info("Detected 3D Skin Layers loaded: {}", ModList.get().isLoaded("skinlayers3d"));
        event.enqueueWork(() -> {
            EmfCompat.registerHooks();
            if (EmotecraftCompatMod.canRegisterSkinLayersBendCompat(ModList.get()::isLoaded)) {
                SkinLayersBendCompat.register();
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(
                (ResourceManagerReloadListener) resourceManager -> PlayerAnimatorBendCompat.clearLayerSources()
        );
    }

    @EventBusSubscriber(modid = EmotecraftCompatMod.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        private GameEvents() {
        }

        @SubscribeEvent
        public static void afterClientTick(ClientTickEvent.Post event) {
            EmoteStateManager.refresh(Minecraft.getInstance());
        }

        @SubscribeEvent
        public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
            if (!event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) {
                return;
            }

            EmfCompat.releasePlayerState(player);
            EmoteStateManager.remove(player.getUUID());
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            clearClientState(event.getPlayer());
        }

        static void clearClientState(Player player) {
            if (Minecraft.getInstance().level != null) {
                for (Player levelPlayer : Minecraft.getInstance().level.players()) {
                    EmfCompat.releasePlayerState(levelPlayer);
                }
            } else if (player != null) {
                EmfCompat.releasePlayerState(player);
            }
            EmoteStateManager.clear();
        }
    }
}
