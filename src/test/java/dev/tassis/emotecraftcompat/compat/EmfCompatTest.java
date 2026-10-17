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
    void tracksPauseAndVanillaRegistrationIndependently() throws Exception {
        for (boolean pauseResult : new boolean[] {false, true}) {
            for (boolean vanillaResult : new boolean[] {false, true}) {
                RegistrationFixture.pauseResult = pauseResult;
                RegistrationFixture.vanillaResult = vanillaResult;
                RegistrationFixture.pauseCalls = 0;
                RegistrationFixture.vanillaCalls = 0;
                EmfCompat.HookCapability pause = capability("pauseCondition", "registerPause");
                EmfCompat.HookCapability vanilla = capability("vanillaModelCondition", "registerVanilla");

                pause.register(ignored -> false);
                vanilla.register(ignored -> false);

                assertEquals(
                        pauseResult ? EmfCompat.CapabilityState.ACTIVE : EmfCompat.CapabilityState.FAILED,
                        pause.state()
                );
                assertEquals(
                        vanillaResult ? EmfCompat.CapabilityState.ACTIVE : EmfCompat.CapabilityState.FAILED,
                        vanilla.state()
                );
                assertEquals(1, RegistrationFixture.pauseCalls);
                assertEquals(1, RegistrationFixture.vanillaCalls);
            }
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
    void isolatesRegistrationExceptionsToTheFailingCapability() throws Exception {
        RegistrationFixture.vanillaCalls = 0;
        RegistrationFixture.vanillaResult = true;
        EmfCompat.HookCapability pause = capability("pauseCondition", "throwDuringRegistration");
        EmfCompat.HookCapability vanilla = capability("vanillaModelCondition", "registerVanilla");

        pause.register(ignored -> false);
        vanilla.register(ignored -> false);

        assertEquals(EmfCompat.CapabilityState.FAILED, pause.state());
        assertEquals(EmfCompat.CapabilityState.ACTIVE, vanilla.state());
        assertEquals(1, RegistrationFixture.vanillaCalls);
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
        private static boolean vanillaResult;
        private static int pauseCalls;
        private static int vanillaCalls;

        private RegistrationFixture() {
        }

        public static boolean registerPause(Function<Object, Boolean> condition) {
            pauseCalls++;
            return pauseResult;
        }

        public static boolean registerVanilla(Function<Object, Boolean> condition) {
            vanillaCalls++;
            return vanillaResult;
        }

        public static boolean throwDuringRegistration(Function<Object, Boolean> condition) {
            throw new IllegalStateException("fixture failure");
        }
    }
}
