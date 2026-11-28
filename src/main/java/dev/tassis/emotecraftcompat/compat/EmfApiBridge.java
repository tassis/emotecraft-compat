package dev.tassis.emotecraftcompat.compat;

import dev.tassis.emotecraftcompat.EmotecraftCompatMod;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Reflection boundary for the optional EMF dependency. EMF names and invocation details live here
 * so {@link EmfCompat} can focus on fallback policy and player state.
 */
final class EmfApiBridge {
    private static final String API_CLASS = "traben.entity_model_features.EMFAnimationApi";
    private static final String ENTITY_CLASS = "traben.entity_model_features.utils.EMFEntity";
    private static final String ANIMATION_CONTEXT_CLASS =
            "traben.entity_model_features.models.animation.EMFAnimationEntityContext";

    private static boolean contextResolved;
    private static Method currentEntityMethod;

    private final Method registerPauseCondition;
    private final Method pauseAnimations;
    private final Method resumeAnimations;

    private EmfApiBridge(
            Method registerPauseCondition,
            Method pauseAnimations,
            Method resumeAnimations
    ) {
        this.registerPauseCondition = registerPauseCondition;
        this.pauseAnimations = pauseAnimations;
        this.resumeAnimations = resumeAnimations;
    }

    static EmfApiBridge unavailable() {
        return new EmfApiBridge(null, null, null);
    }

    static EmfApiBridge resolve(ClassLoader classLoader) throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName(API_CLASS, false, classLoader);
        Method pauseCondition = findMethod(apiClass, "registerPauseCondition", Function.class);
        Method pause = null;
        Method resume = null;
        try {
            Class<?> entityClass = Class.forName(ENTITY_CLASS, false, classLoader);
            pause = findMethod(apiClass, "pauseAllCustomAnimationsForEntity", entityClass);
            resume = findMethod(apiClass, "resumeAllCustomAnimationsForEntity", entityClass);
        } catch (ReflectiveOperationException exception) {
            // Modern condition hooks can still work when only the legacy entity API is unavailable.
            EmotecraftCompatMod.LOGGER.warn("Failed to resolve the legacy EMF entity API", exception);
        }

        return new EmfApiBridge(pauseCondition, pause, resume);
    }

    Method pauseConditionRegistration() {
        return registerPauseCondition;
    }

    boolean pauseAnimations(Player player) throws ReflectiveOperationException {
        return invoke(pauseAnimations, player);
    }

    boolean resumeAnimations(Player player) throws ReflectiveOperationException {
        return invoke(resumeAnimations, player);
    }

    static Player currentAnimatedPlayer() {
        resolveAnimationContext();
        if (currentEntityMethod == null) {
            return null;
        }

        try {
            Object entity = currentEntityMethod.invoke(null);
            return entity instanceof Player player ? player : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static synchronized void resolveAnimationContext() {
        if (contextResolved) {
            return;
        }

        contextResolved = true;
        try {
            currentEntityMethod = Class.forName(ANIMATION_CONTEXT_CLASS).getMethod("getEMFEntity");
        } catch (ReflectiveOperationException | SecurityException exception) {
            currentEntityMethod = null;
        }
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (ReflectiveOperationException | SecurityException exception) {
            return null;
        }
    }

    private static boolean invoke(Method method, Player player) throws ReflectiveOperationException {
        return method != null && Boolean.TRUE.equals(method.invoke(null, player));
    }
}
