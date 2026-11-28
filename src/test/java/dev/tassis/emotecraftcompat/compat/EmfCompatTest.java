package dev.tassis.emotecraftcompat.compat;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmfCompatTest {
    @Test
    void tracksPauseRegistration() throws Exception {
        for (boolean pauseResult : new boolean[] {false, true}) {
            RegistrationFixture.pauseResult = pauseResult;
            RegistrationFixture.pauseCalls = 0;
            EmfCompat.HookCapability pause = capability("pauseCondition", "registerPause");
            pause.register(ignored -> false);
            assertEquals(pauseResult ? EmfCompat.CapabilityState.ACTIVE : EmfCompat.CapabilityState.FAILED,
                    pause.state());
            assertEquals(pauseResult, pause.isActive());
            assertEquals(1, RegistrationFixture.pauseCalls);
        }
    }

    @Test
    void doesNotRepeatSuccessfulOrFailedRegistrationAttempts() throws Exception {
        RegistrationFixture.pauseCalls = 0;
        RegistrationFixture.pauseResult = true;
        EmfCompat.HookCapability active = capability("pauseCondition", "registerPause");
        active.register(ignored -> false);
        active.register(ignored -> false);
        assertEquals(EmfCompat.CapabilityState.ACTIVE, active.state());
        assertEquals(1, RegistrationFixture.pauseCalls);

        RegistrationFixture.pauseCalls = 0;
        RegistrationFixture.pauseResult = false;
        EmfCompat.HookCapability failed = capability("pauseCondition", "registerPause");
        failed.register(ignored -> false);
        failed.register(ignored -> false);
        assertEquals(EmfCompat.CapabilityState.FAILED, failed.state());
        assertEquals(1, RegistrationFixture.pauseCalls);
    }

    @Test
    void failedRegistrationAllowsFallback() throws Exception {
        EmfCompat.HookCapability pause = capability("pauseCondition", "throwDuringRegistration");
        pause.register(ignored -> false);
        assertEquals(EmfCompat.CapabilityState.FAILED, pause.state());
        assertFalse(pause.isActive());
    }

    @Test
    void missingPauseApiAllowsFallback() {
        EmfCompat.HookCapability pause = EmfCompat.HookCapability.unavailable("pauseCondition");
        assertEquals(EmfCompat.CapabilityState.UNAVAILABLE, pause.register(ignored -> false));
        assertFalse(pause.isAvailable());
        assertFalse(pause.isActive());
    }

    @Test
    void recordsLegacyApplyOnlyAfterSuccessAndRetriesFailure() throws Exception {
        UUID playerId = UUID.randomUUID();
        Set<UUID> applied = new HashSet<>();
        AtomicBoolean succeeds = new AtomicBoolean(false);
        AtomicInteger applyCalls = new AtomicInteger();

        assertFalse(EmfCompat.updateLegacyState(
                applied,
                playerId,
                true,
                () -> {
                    applyCalls.incrementAndGet();
                    return succeeds.get();
                }
        ));
        assertFalse(applied.contains(playerId));

        succeeds.set(true);
        assertTrue(EmfCompat.updateLegacyState(
                applied,
                playerId,
                true,
                () -> {
                    applyCalls.incrementAndGet();
                    return true;
                }
        ));
        assertTrue(applied.contains(playerId));

        assertTrue(EmfCompat.updateLegacyState(applied, playerId, true, () -> {
            applyCalls.incrementAndGet();
            return true;
        }));
        assertEquals(2, applyCalls.get(), "already-applied state must not invoke the operation again");
    }

    @Test
    void removesLegacyStateOnlyAfterSuccessfulRelease() throws Exception {
        UUID playerId = UUID.randomUUID();
        Set<UUID> applied = new HashSet<>();
        AtomicBoolean succeeds = new AtomicBoolean(false);
        AtomicInteger releaseCalls = new AtomicInteger();

        assertTrue(EmfCompat.updateLegacyState(applied, playerId, false, () -> {
            releaseCalls.incrementAndGet();
            return false;
        }));
        assertEquals(0, releaseCalls.get(), "already-released state must not invoke the operation");

        applied.add(playerId);

        assertFalse(EmfCompat.updateLegacyState(
                applied,
                playerId,
                false,
                () -> {
                    releaseCalls.incrementAndGet();
                    return succeeds.get();
                }
        ));
        assertTrue(applied.contains(playerId));

        succeeds.set(true);
        assertTrue(EmfCompat.updateLegacyState(
                applied,
                playerId,
                false,
                () -> {
                    releaseCalls.incrementAndGet();
                    return succeeds.get();
                }
        ));
        assertFalse(applied.contains(playerId));
        assertEquals(2, releaseCalls.get());
    }

    @Test
    void leavesAppliedStateIntactWhenOperationThrows() {
        UUID playerId = UUID.randomUUID();
        Set<UUID> applied = new HashSet<>();
        applied.add(playerId);

        assertThrows(ReflectiveOperationException.class, () -> EmfCompat.updateLegacyState(
                applied,
                playerId,
                false,
                () -> {
                    throw new ReflectiveOperationException("fixture failure");
                }
        ));
        assertTrue(applied.contains(playerId));
    }

    private static EmfCompat.HookCapability capability(String name, String methodName) throws Exception {
        Method method = RegistrationFixture.class.getMethod(methodName, Function.class);
        return new EmfCompat.HookCapability(name, method);
    }

    public static final class RegistrationFixture {
        private static boolean pauseResult;
        private static int pauseCalls;

        private RegistrationFixture() {
        }

        public static boolean registerPause(Function<Object, Boolean> condition) {
            pauseCalls++;
            return pauseResult;
        }

        public static boolean throwDuringRegistration(Function<Object, Boolean> condition) {
            throw new IllegalStateException("fixture failure");
        }
    }
}
