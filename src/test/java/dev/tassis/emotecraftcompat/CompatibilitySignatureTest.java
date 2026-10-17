package dev.tassis.emotecraftcompat;

import dev.tassis.emotecraftcompat.mixin.OptionalCompatMixinPlugin;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilitySignatureTest {
    @Test
    void emotecraftExposesActiveEmoteState() throws Exception {
        Class<?> playerEntity = load("io.github.kosmx.emotes.main.mixinFunctions.IPlayerEntity");

        assertEquals(boolean.class, playerEntity.getMethod("isPlayingEmote").getReturnType());
    }

    @Test
    void emfExposesModernConditionsAndLegacyPauseHook() throws Exception {
        Class<?> api = load("traben.entity_model_features.EMFAnimationApi");
        Class<?> context = load("traben.entity_model_features.models.animation.EMFAnimationEntityContext");

        assertEquals(boolean.class, api.getMethod("registerPauseCondition", Function.class).getReturnType());
        assertEquals(boolean.class, api.getMethod("registerVanillaModelCondition", Function.class).getReturnType());
        assertEquals(boolean.class, context.getMethod("isEntityAnimPaused").getReturnType());
        assertThrows(NoSuchMethodException.class, () -> context.getMethod("isEntityAnimPausedWrapped"));
        assertThrows(NoSuchMethodException.class, () -> context.getMethod("isEntityForcedToVanillaModel"));
    }

    @Test
    void playerAnimatorMixinTargetsMatchNeoForgeDescriptors() throws Exception {
        Class<?> applier = load("dev.kosmx.playerAnim.impl.animation.AnimationApplier");
        Class<?> bendHelper = load("dev.kosmx.playerAnim.impl.animation.BendHelper");

        Method updatePart = applier.getMethod("updatePart", String.class, ModelPart.class);
        Method bend = bendHelper.getMethod("bend", ModelPart.class, float.class, float.class);
        assertNotNull(updatePart);
        assertNotNull(bend);

        OptionalCompatMixinPlugin plugin = new OptionalCompatMixinPlugin();
        assertTrue(plugin.shouldApplyMixin(
                applier.getName(),
                "dev.tassis.emotecraftcompat.mixin.PlayerAnimatorAnimationApplierMixin"
        ));
        assertTrue(plugin.shouldApplyMixin(
                bendHelper.getName(),
                "dev.tassis.emotecraftcompat.mixin.PlayerAnimatorBendHelperMixin"
        ));
    }

    @Test
    void skinLayersExposesBendTransformerApi() throws Exception {
        Class<?> api = load("dev.tr7zw.skinlayers.api.SkinLayersAPI");
        Class<?> layerApi = load("dev.tr7zw.skinlayers.api.LayerFeatureTransformerAPI");
        Class<?> layerTransformer = load("dev.tr7zw.skinlayers.api.LayerFeatureTransformerAPI$LayerTransformer");
        Class<?> provider = load("dev.tr7zw.skinlayers.api.MeshTransformerProvider");
        Class<?> transformer = load("dev.tr7zw.skinlayers.api.MeshTransformer");

        assertEquals(void.class, api.getMethod("setupMeshTransformerProvider", provider).getReturnType());
        assertEquals(provider, api.getMethod("getMeshTransformerProvider").getReturnType());
        assertEquals(void.class, layerApi.getMethod("setLayerTransformer", layerTransformer).getReturnType());
        assertEquals(layerTransformer, layerApi.getMethod("getTransformer").getReturnType());
        assertEquals(transformer, provider.getMethod("prepareTransformer", ModelPart.class).getReturnType());
    }

    private static Class<?> load(String className) throws ClassNotFoundException {
        return Class.forName(className, false, CompatibilitySignatureTest.class.getClassLoader());
    }
}
