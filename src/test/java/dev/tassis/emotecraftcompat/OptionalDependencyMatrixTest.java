package dev.tassis.emotecraftcompat;

import dev.tassis.emotecraftcompat.mixin.OptionalCompatMixinPlugin;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionalDependencyMatrixTest {
    private static final String APPLIER_MIXIN = "compat.PlayerAnimatorAnimationApplierMixin";
    private static final String BEND_HELPER_MIXIN = "compat.PlayerAnimatorBendHelperMixin";
    private static final String PLAYER_MODEL_LAYER_MIXIN = "compat.PlayerModelLayerBendMixin";

    @Test
    void onlyEmotecraftLoadsNoOptionalCompatibility() {
        Predicate<String> loaded = loaded("emotecraft");

        assertBendMixins(false, loaded);
    }

    @Test
    void partialPlayerAnimationStacksStayDisabled() {
        assertBendMixins(false, loaded("emotecraft", "playeranimator"));
        assertBendMixins(false, loaded("emotecraft", "bendylib"));
    }

    @Test
    void playerAnimatorAndBendyLibEnableBendRepair() {
        assertBendMixins(true, loaded("emotecraft", "playeranimator", "bendylib"));
    }

    private static void assertBendMixins(boolean expected, Predicate<String> loaded) {
        if (expected) {
            assertTrue(applies(APPLIER_MIXIN, loaded));
            assertTrue(applies(BEND_HELPER_MIXIN, loaded));
            assertTrue(applies(PLAYER_MODEL_LAYER_MIXIN, loaded));
        } else {
            assertFalse(applies(APPLIER_MIXIN, loaded));
            assertFalse(applies(BEND_HELPER_MIXIN, loaded));
            assertFalse(applies(PLAYER_MODEL_LAYER_MIXIN, loaded));
        }
    }

    private static boolean applies(String mixin, Predicate<String> loaded) {
        return OptionalCompatMixinPlugin.shouldApplyMixin(mixin, loaded);
    }

    private static Predicate<String> loaded(String... modIds) {
        Set<String> loadedMods = Set.of(modIds);
        return loadedMods::contains;
    }
}
