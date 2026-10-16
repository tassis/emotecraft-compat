package dev.tassis.emotecraftcompat.compat;

import dev.tassis.emotecraftcompat.EmotecraftCompatMod;
import dev.tassis.emotecraftcompat.state.EmoteStateManager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class EmfCompat {
    private static final String EMF_MOD_ID = "entity_model_features";
    private static final String EMF_CONTEXT = "traben.entity_model_features.models.animation.EMFAnimationEntityContext";
    private static final String EMF_API = "traben.entity_model_features.EMFAnimationApi";
    private static final String EMF_ENTITY = "traben.entity_model_features.utils.EMFEntity";

    private static final Map<UUID, Boolean> ACTIVE_EMF_SUPPRESSION = new ConcurrentHashMap<>();
    private static boolean contextResolved;
    private static Method getCurrentEntityMethod;
    private static boolean apiResolved;
    private static boolean hooksRegistered;
    private static boolean modernApiAvailable;
    private static Method registerPauseConditionMethod;
    private static Method registerVanillaModelConditionMethod;
    private static Method lockEntityToVanillaModelMethod;
    private static Method unlockEntityToVanillaModelMethod;
    private static Method pauseAllCustomAnimationsForEntityMethod;
    private static Method resumeAllCustomAnimationsForEntityMethod;

    private EmfCompat() {
    }

    public static void registerHooks() {
        if (!isLoaded()) {
            return;
        }

        resolveApi();
        if (!modernApiAvailable) {
            EmotecraftCompatMod.LOGGER.info("Modern EMF condition API unavailable; legacy compatibility fallback will be used");
            return;
        }
        if (hooksRegistered) {
            return;
        }

        try {
            // EMF calls these from rendering; only consume tick-captured state here.
            Function<Object, Boolean> condition = EmfCompat::isTrackedEmotingPlayer;
            boolean pauseRegistered = Boolean.TRUE.equals(registerPauseConditionMethod.invoke(null, condition));
            boolean vanillaRegistered = Boolean.TRUE.equals(registerVanillaModelConditionMethod.invoke(null, condition));
            hooksRegistered = pauseRegistered && vanillaRegistered;
            EmotecraftCompatMod.LOGGER.info(
                    "Registered EMF compatibility hooks: pauseCondition={}, vanillaModelCondition={}",
                    pauseRegistered,
                    vanillaRegistered
            );
        } catch (ReflectiveOperationException exception) {
            EmotecraftCompatMod.LOGGER.warn("Failed to register EMF compatibility hooks", exception);
        }
    }

    public static void syncPlayerAnimationState(Player player, boolean emoting) {
        if (!isLoaded() || !apiResolved || modernApiAvailable) {
            return;
        }
        if (pauseAllCustomAnimationsForEntityMethod == null
                && resumeAllCustomAnimationsForEntityMethod == null
                && lockEntityToVanillaModelMethod == null
                && unlockEntityToVanillaModelMethod == null) {
            return;
        }

        UUID playerId = player.getUUID();
        Boolean previous = ACTIVE_EMF_SUPPRESSION.put(playerId, emoting);
        if (previous != null && previous.booleanValue() == emoting) {
            return;
        }

        try {
            if (emoting) {
                invokeEntityOperation(pauseAllCustomAnimationsForEntityMethod, player);
                invokeEntityOperation(lockEntityToVanillaModelMethod, player);
            } else {
                invokeEntityOperation(resumeAllCustomAnimationsForEntityMethod, player);
                invokeEntityOperation(unlockEntityToVanillaModelMethod, player);
            }
        } catch (ReflectiveOperationException exception) {
            EmotecraftCompatMod.LOGGER.warn("Failed to sync EMF suppression state for player {}", playerId, exception);
        }
    }

    public static void forgetPlayerState(UUID playerId) {
        ACTIVE_EMF_SUPPRESSION.remove(playerId);
    }

    public static void clearTrackedStates() {
        ACTIVE_EMF_SUPPRESSION.clear();
    }

    public static boolean shouldSuppressCurrentAnimatedPlayer() {
        Player player = getCurrentAnimatedPlayer();
        return player != null && EmoteStateManager.isEmoteActive(player.getUUID());
    }

    public static boolean isModernApiAvailable() {
        return apiResolved && modernApiAvailable;
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

        resolveContext();
        if (getCurrentEntityMethod == null) {
            return null;
        }

        try {
            Object emfEntity = getCurrentEntityMethod.invoke(null);
            return emfEntity instanceof Player player ? player : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static void resolveContext() {
        if (contextResolved) {
            return;
        }

        contextResolved = true;
        try {
            getCurrentEntityMethod = Class.forName(EMF_CONTEXT).getMethod("getEMFEntity");
        } catch (ReflectiveOperationException exception) {
            getCurrentEntityMethod = null;
        }
    }

    private static void resolveApi() {
        if (apiResolved) {
            return;
        }

        apiResolved = true;
        try {
            ClassLoader classLoader = EmfCompat.class.getClassLoader();
            Class<?> apiClass = Class.forName(EMF_API, false, classLoader);
            registerPauseConditionMethod = tryResolve(apiClass, "registerPauseCondition", Function.class);
            registerVanillaModelConditionMethod = tryResolve(apiClass, "registerVanillaModelCondition", Function.class);
            modernApiAvailable = registerPauseConditionMethod != null && registerVanillaModelConditionMethod != null;

            if (!modernApiAvailable) {
                Class<?> emfEntityClass = Class.forName(EMF_ENTITY, false, classLoader);
                lockEntityToVanillaModelMethod = tryResolve(apiClass, "lockEntityToVanillaModel", emfEntityClass);
                unlockEntityToVanillaModelMethod = tryResolve(apiClass, "unlockEntityToVanillaModel", emfEntityClass);
                pauseAllCustomAnimationsForEntityMethod = tryResolve(apiClass, "pauseAllCustomAnimationsForEntity", emfEntityClass);
                resumeAllCustomAnimationsForEntityMethod = tryResolve(apiClass, "resumeAllCustomAnimationsForEntity", emfEntityClass);
            }
        } catch (ReflectiveOperationException exception) {
            modernApiAvailable = false;
            EmotecraftCompatMod.LOGGER.warn("Failed to load the EMF animation API; mixin fallback will be used", exception);
        }
    }

    private static Method tryResolve(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (ReflectiveOperationException | SecurityException exception) {
            return null;
        }
    }

    private static boolean invokeEntityOperation(Method method, Player player) throws ReflectiveOperationException {
        return method != null && Boolean.TRUE.equals(method.invoke(null, player));
    }
}
