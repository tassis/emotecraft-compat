package dev.tassis.emotecraftcompat;

import dev.tassis.emotecraftcompat.compat.EmfCompat;
import dev.tassis.emotecraftcompat.state.EmoteStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

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
        event.enqueueWork(EmfCompat::registerHooks);
    }

    @EventBusSubscriber(modid = EmotecraftCompatMod.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        private GameEvents() {
        }

        @SubscribeEvent
        public static void afterClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                EmoteStateManager.refresh(Minecraft.getInstance());
            }
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
