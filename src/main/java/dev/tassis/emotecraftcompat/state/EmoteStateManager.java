package dev.tassis.emotecraftcompat.state;

import dev.tassis.emotecraftcompat.EmotecraftCompatMod;
import dev.tassis.emotecraftcompat.compat.EmfCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EmoteStateManager {
    private static final String EMOTECRAFT_MOD_ID = "emotecraft";
    private static final Map<UUID, Boolean> ACTIVE_EMOTES = new ConcurrentHashMap<>();

    private EmoteStateManager() {
    }

    public static void refresh(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }

        Set<UUID> seenPlayers = new HashSet<>();
        for (AbstractClientPlayer player : level.players()) {
            seenPlayers.add(player.getUUID());
            recordPlayerState(player, isPlayerEmoting(player));
        }

        ACTIVE_EMOTES.keySet().removeIf(uuid -> !seenPlayers.contains(uuid));
    }

    public static boolean isEmoteActive(UUID playerId) {
        return ACTIVE_EMOTES.getOrDefault(playerId, false);
    }

    public static void recordPlayerState(Player player, boolean isActive) {
        UUID uuid = player.getUUID();
        Boolean previous = ACTIVE_EMOTES.put(uuid, isActive);
        EmfCompat.syncPlayerAnimationState(player, isActive);
        if (previous == null || previous.booleanValue() != isActive) {
            EmotecraftCompatMod.LOGGER.info(
                    "Detected emote state change for player {} ({}): active={}",
                    player.getScoreboardName(),
                    uuid,
                    isActive
            );
        }
    }

    public static void remove(UUID playerId) {
        ACTIVE_EMOTES.remove(playerId);
        EmfCompat.forgetPlayerState(playerId);
    }

    public static void clear() {
        ACTIVE_EMOTES.clear();
        EmfCompat.clearTrackedStates();
    }

    private static boolean isPlayerEmoting(Player player) {
        return ModList.get().isLoaded(EMOTECRAFT_MOD_ID) && EmotecraftAccess.isPlayerEmoting(player);
    }

    private static final class EmotecraftAccess {
        private EmotecraftAccess() {
        }

        private static boolean isPlayerEmoting(Player player) {
            return player instanceof io.github.kosmx.emotes.main.mixinFunctions.IPlayerEntity emotePlayer
                    && emotePlayer.isPlayingEmote();
        }
    }
}
