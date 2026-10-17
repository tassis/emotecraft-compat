package dev.tassis.emotecraftcompat.compat;

import dev.tassis.emotecraftcompat.EmotecraftCompatMod;
import dev.tassis.emotecraftcompat.state.EmoteStateManager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class EmfCompat {
    private static final String EMF_MOD_ID = "entity_model_features";

    // These sets record successful legacy API calls. Entries are removed only after the matching
    // release succeeds, so a transient EMF failure can be retried instead of silently leaking state.
    private static final Set<UUID> LEGACY_PAUSED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> LEGACY_VANILLA_PLAYERS = ConcurrentHashMap.newKeySet();
    private static volatile boolean apiResolved;
    private static volatile EmfApiBridge api = EmfApiBridge.unavailable();
    private static volatile HookCapability pauseHook = HookCapability.unavailable("pauseCondition");
    private static volatile HookCapability vanillaModelHook = HookCapability.unavailable("vanillaModelCondition");

    private EmfCompat() {
    }

    public static void registerHooks() {
        if (!isLoaded()) {
            return;
        }

        resolveApi();
        if (!pauseHook.isAvailable() && !vanillaModelHook.isAvailable()) {
            EmotecraftCompatMod.LOGGER.info("Modern EMF condition API unavailable; legacy compatibility fallback will be used");
            return;
        }

        // EMF calls these from rendering; only consume tick-captured state here.
        Function<Object, Boolean> condition = EmfCompat::isTrackedEmotingPlayer;
        pauseHook.register(condition);
        vanillaModelHook.register(condition);
        EmotecraftCompatMod.LOGGER.info(
                "Registered EMF compatibility hooks: pauseCondition={}, vanillaModelCondition={}",
                pauseHook.state(),
                vanillaModelHook.state()
        );
    }

    public static void syncPlayerAnimationState(Player player, boolean emoting) {
        if (!isLoaded() || !apiResolved) {
            return;
        }

        UUID playerId = player.getUUID();
        if (shouldUseLegacyPauseFallback()) {
            syncLegacyCapability(
                    "pause",
                    LEGACY_PAUSED_PLAYERS,
                    playerId,
                    emoting,
                    () -> emoting ? api.pauseAnimations(player) : api.resumeAnimations(player)
            );
        }
        if (shouldUseLegacyVanillaFallback()) {
            syncLegacyCapability(
                    "vanilla model",
                    LEGACY_VANILLA_PLAYERS,
                    playerId,
                    emoting,
                    () -> emoting ? api.lockVanillaModel(player) : api.unlockVanillaModel(player)
            );
        }
    }

    public static void releasePlayerState(Player player) {
        UUID playerId = player.getUUID();
        syncLegacyCapability(
                "pause",
                LEGACY_PAUSED_PLAYERS,
                playerId,
                false,
                () -> api.resumeAnimations(player)
        );
        syncLegacyCapability(
                "vanilla model",
                LEGACY_VANILLA_PLAYERS,
                playerId,
                false,
                () -> api.unlockVanillaModel(player)
        );
    }

    public static void warnIfLegacyStateRemains(UUID playerId) {
        if (LEGACY_PAUSED_PLAYERS.contains(playerId) || LEGACY_VANILLA_PLAYERS.contains(playerId)) {
            EmotecraftCompatMod.LOGGER.warn(
                    "Retaining failed EMF release state for player {} so it can be retried",
                    playerId
            );
        }
    }

    public static void warnIfLegacyStatesRemain() {
        if (!LEGACY_PAUSED_PLAYERS.isEmpty() || !LEGACY_VANILLA_PLAYERS.isEmpty()) {
            EmotecraftCompatMod.LOGGER.warn(
                    "EMF legacy suppression remains applied for {} paused and {} vanilla-model players",
                    LEGACY_PAUSED_PLAYERS.size(),
                    LEGACY_VANILLA_PLAYERS.size()
            );
        }
    }

    public static boolean shouldSuppressCurrentAnimatedPlayer() {
        Player player = getCurrentAnimatedPlayer();
        return player != null && EmoteStateManager.isEmoteActive(player.getUUID());
    }

    public static boolean shouldUseLegacyPauseFallback() {
        return !pauseHook.isActive();
    }

    public static boolean shouldUseLegacyVanillaFallback() {
        return !vanillaModelHook.isActive();
    }

    private static boolean isLoaded() {
        return ModList.get().isLoaded(EMF_MOD_ID);
    }

    private static boolean isTrackedEmotingPlayer(Object emfEntity) {
        return emfEntity instanceof Player player && EmoteStateManager.isEmoteActive(player.getUUID());
    }

    private static Player getCurrentAnimatedPlayer() {
        if (!isLoaded()) {
            return null;
        }
        return EmfApiBridge.currentAnimatedPlayer();
    }

    private static synchronized void resolveApi() {
        if (apiResolved) {
            return;
        }

        try {
            api = EmfApiBridge.resolve(EmfCompat.class.getClassLoader());
            pauseHook = new HookCapability(
                    "pauseCondition",
                    api.pauseConditionRegistration()
            );
            vanillaModelHook = new HookCapability(
                    "vanillaModelCondition",
                    api.vanillaModelConditionRegistration()
            );
        } catch (ReflectiveOperationException exception) {
            EmotecraftCompatMod.LOGGER.warn("Failed to load the EMF animation API; mixin fallback will be used", exception);
        } finally {
            apiResolved = true;
        }
    }

    private static void syncLegacyCapability(
            String capability,
            Set<UUID> appliedPlayers,
            UUID playerId,
            boolean emoting,
            LegacyOperation transition
    ) {
        try {
            updateLegacyState(
                    appliedPlayers,
                    playerId,
                    emoting,
                    transition
            );
        } catch (ReflectiveOperationException | RuntimeException exception) {
            EmotecraftCompatMod.LOGGER.warn(
                    "Failed to sync EMF {} suppression state for player {}",
                    capability,
                    playerId,
                    exception
            );
        }
    }

    static synchronized boolean updateLegacyState(
            Set<UUID> appliedPlayers,
            UUID playerId,
            boolean shouldApply,
            LegacyOperation transition
    ) throws ReflectiveOperationException {
        if (appliedPlayers.contains(playerId) == shouldApply) {
            return true;
        }

        if (!transition.invoke()) {
            return false;
        }
        if (shouldApply) {
            appliedPlayers.add(playerId);
        } else {
            appliedPlayers.remove(playerId);
        }
        return true;
    }

    enum CapabilityState {
        UNAVAILABLE,
        UNATTEMPTED,
        ACTIVE,
        FAILED
    }

    static final class HookCapability {
        private final String name;
        private final Method registrationMethod;
        private volatile CapabilityState state;

        HookCapability(String name, Method registrationMethod) {
            this.name = name;
            this.registrationMethod = registrationMethod;
            state = registrationMethod == null ? CapabilityState.UNAVAILABLE : CapabilityState.UNATTEMPTED;
        }

        static HookCapability unavailable(String name) {
            return new HookCapability(name, null);
        }

        synchronized CapabilityState register(Function<Object, Boolean> condition) {
            if (state != CapabilityState.UNATTEMPTED) {
                return state;
            }

            try {
                state = Boolean.TRUE.equals(registrationMethod.invoke(null, condition))
                        ? CapabilityState.ACTIVE
                        : CapabilityState.FAILED;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                state = CapabilityState.FAILED;
                EmotecraftCompatMod.LOGGER.warn("Failed to register EMF {} hook", name, exception);
            }
            return state;
        }

        CapabilityState state() {
            return state;
        }

        boolean isAvailable() {
            return state != CapabilityState.UNAVAILABLE;
        }

        boolean isActive() {
            return state == CapabilityState.ACTIVE;
        }
    }

    @FunctionalInterface
    interface LegacyOperation {
        boolean invoke() throws ReflectiveOperationException;
    }

}
