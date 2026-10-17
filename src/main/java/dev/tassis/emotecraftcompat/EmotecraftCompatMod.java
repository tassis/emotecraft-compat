package dev.tassis.emotecraftcompat;

import com.mojang.logging.LogUtils;
import dev.tassis.emotecraftcompat.compat.EmfCompat;
import dev.tassis.emotecraftcompat.compat.PlayerAnimatorBendCompat;
import dev.tassis.emotecraftcompat.compat.SkinLayersBendCompat;
import dev.tassis.emotecraftcompat.state.EmoteStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.slf4j.Logger;

import java.util.function.Predicate;

@Mod(value = EmotecraftCompatMod.MOD_ID, dist = Dist.CLIENT)
public final class EmotecraftCompatMod {
    public static final String MOD_ID = "emotecraft_compat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EmotecraftCompatMod(IEventBus modEventBus) {
        LOGGER.info("Initializing {}", MOD_ID);
        modEventBus.addListener(EmotecraftCompatMod::onClientSetup);
        modEventBus.addListener(EmotecraftCompatMod::onRegisterClientReloadListeners);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Detected Emotecraft loaded: {}", ModList.get().isLoaded("emotecraft"));
        LOGGER.info("Detected EMF loaded: {}", ModList.get().isLoaded("entity_model_features"));
        LOGGER.info("Detected 3D Skin Layers loaded: {}", ModList.get().isLoaded("skinlayers3d"));
        event.enqueueWork(() -> {
            EmfCompat.registerHooks();
            if (canRegisterSkinLayersBendCompat(modId -> ModList.get().isLoaded(modId))) {
                SkinLayersBendCompat.register();
            }
        });
    }

    private static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(
                (ResourceManagerReloadListener) resourceManager -> PlayerAnimatorBendCompat.clearLayerSources()
        );
    }

    public static boolean canRegisterSkinLayersBendCompat(Predicate<String> isLoaded) {
        return isLoaded.test("skinlayers3d")
                && isLoaded.test("playeranimator")
                && isLoaded.test("bendylib");
    }

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
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
