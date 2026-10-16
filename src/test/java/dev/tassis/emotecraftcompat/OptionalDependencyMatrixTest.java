package dev.tassis.emotecraftcompat;

import dev.tassis.emotecraftcompat.mixin.OptionalCompatMixinPlugin;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionalDependencyMatrixTest {
    private static final String EMF_MIXIN = "compat.EMFAnimationEntityContextMixin";
    private static final String LAYER_MIXIN = "compat.PlayerModelLayerBendMixin";
    private static final String APPLIER_MIXIN = "compat.PlayerAnimatorAnimationApplierMixin";
    private static final String BEND_HELPER_MIXIN = "compat.PlayerAnimatorBendHelperMixin";

    @Test
    void onlyEmotecraftLoadsNoOptionalCompatibility() {
        Predicate<String> loaded = loaded("emotecraft");

        assertFalse(applies(EMF_MIXIN, loaded));
        assertBendMixins(false, loaded);
        assertFalse(EmotecraftCompatMod.canRegisterSkinLayersBendCompat(loaded));
    }

    @Test
    void emfWorksWithoutThePlayerAnimationStack() {
        Predicate<String> loaded = loaded("emotecraft", "entity_model_features");

        assertTrue(applies(EMF_MIXIN, loaded));
        assertBendMixins(false, loaded);
        assertFalse(EmotecraftCompatMod.canRegisterSkinLayersBendCompat(loaded));
    }

    @Test
    void partialPlayerAnimationStacksStayDisabled() {
        Predicate<String> playerAnimatorOnly = loaded("emotecraft", "playeranimator");
        Predicate<String> bendyLibOnly = loaded("emotecraft", "bendylib");

        assertBendMixins(false, playerAnimatorOnly);
        assertBendMixins(false, bendyLibOnly);
        assertFalse(EmotecraftCompatMod.canRegisterSkinLayersBendCompat(playerAnimatorOnly));
        assertFalse(EmotecraftCompatMod.canRegisterSkinLayersBendCompat(bendyLibOnly));
    }

    @Test
    void playerAnimatorAndBendyLibEnableVanillaLayerRepair() {
        Predicate<String> loaded = loaded("emotecraft", "playeranimator", "bendylib");

        assertBendMixins(true, loaded);
        assertFalse(EmotecraftCompatMod.canRegisterSkinLayersBendCompat(loaded));
    }

    @Test
    void skinLayersBendRequiresTheCompleteStack() {
        assertFalse(EmotecraftCompatMod.canRegisterSkinLayersBendCompat(
                loaded("emotecraft", "skinlayers3d", "playeranimator")
        ));
        assertFalse(EmotecraftCompatMod.canRegisterSkinLayersBendCompat(
                loaded("emotecraft", "skinlayers3d", "bendylib")
        ));
        assertTrue(EmotecraftCompatMod.canRegisterSkinLayersBendCompat(
                loaded("emotecraft", "skinlayers3d", "playeranimator", "bendylib")
        ));
    }

    private static void assertBendMixins(boolean expected, Predicate<String> loaded) {
        if (expected) {
            assertTrue(applies(LAYER_MIXIN, loaded));
            assertTrue(applies(APPLIER_MIXIN, loaded));
            assertTrue(applies(BEND_HELPER_MIXIN, loaded));
        } else {
            assertFalse(applies(LAYER_MIXIN, loaded));
            assertFalse(applies(APPLIER_MIXIN, loaded));
            assertFalse(applies(BEND_HELPER_MIXIN, loaded));
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
