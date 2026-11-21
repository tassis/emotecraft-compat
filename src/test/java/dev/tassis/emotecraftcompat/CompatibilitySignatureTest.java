package dev.tassis.emotecraftcompat;

import dev.tassis.emotecraftcompat.mixin.OptionalCompatMixinPlugin;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilitySignatureTest {
    @Test
    void emotecraftExposesActiveEmoteState() throws Exception {
        Class<?> playerEntity = load("io.github.kosmx.emotes.main.mixinFunctions.IPlayerEntity");

        assertEquals(boolean.class, playerEntity.getMethod("isPlayingEmote").getReturnType());
    }

    @Test
    void emfExposesConditionAndEntityControlApis() throws IOException {
        ClassNode api = classNode("traben/entity_model_features/EMFAnimationApi");
        String conditionDescriptor = "(Ljava/util/function/Function;)Z";
        String entityDescriptor = "(Ltraben/entity_model_features/utils/EMFEntity;)Z";

        assertTrue(hasMethod(api, "registerPauseCondition", conditionDescriptor));
        assertTrue(hasMethod(api, "registerVanillaModelCondition", conditionDescriptor));
        assertTrue(hasMethod(api, "pauseAllCustomAnimationsForEntity", entityDescriptor));
        assertTrue(hasMethod(api, "resumeAllCustomAnimationsForEntity", entityDescriptor));
        assertTrue(hasMethod(api, "lockEntityToVanillaModel", entityDescriptor));
        assertTrue(hasMethod(api, "unlockEntityToVanillaModel", entityDescriptor));
    }

    @Test
    void playerAnimatorMixinTargetsMatchForgeDescriptors() throws Exception {
        Class<?> applier = load("dev.kosmx.playerAnim.impl.animation.AnimationApplier");
        Class<?> bendHelper = load("dev.kosmx.playerAnim.impl.animation.BendHelper");

        Method updatePart = applier.getMethod("updatePart", String.class, ModelPart.class);
        Method bend = bendHelper.getMethod("bend", ModelPart.class, float.class, float.class);
        assertNotNull(updatePart);
        assertNotNull(bend);

        Predicate<String> loaded = Set.of("playeranimator", "bendylib")::contains;
        assertTrue(OptionalCompatMixinPlugin.shouldApplyMixin(
                "dev.tassis.emotecraftcompat.mixin.PlayerAnimatorAnimationApplierMixin",
                loaded
        ));
        assertTrue(OptionalCompatMixinPlugin.shouldApplyMixin(
                "dev.tassis.emotecraftcompat.mixin.PlayerAnimatorBendHelperMixin",
                loaded
        ));
        assertTrue(OptionalCompatMixinPlugin.shouldApplyMixin(
                "dev.tassis.emotecraftcompat.mixin.PlayerModelLayerBendMixin",
                loaded
        ));
    }

    private static Class<?> load(String className) throws ClassNotFoundException {
        return Class.forName(className, false, CompatibilitySignatureTest.class.getClassLoader());
    }

    private static ClassNode classNode(String internalName) throws IOException {
        String resource = internalName + ".class";
        try (InputStream stream = CompatibilitySignatureTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing classpath resource " + resource);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static boolean hasMethod(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream().anyMatch(method -> method.name.equals(name) && method.desc.equals(descriptor));
    }
}
