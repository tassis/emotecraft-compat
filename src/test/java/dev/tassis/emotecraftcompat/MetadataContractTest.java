package dev.tassis.emotecraftcompat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataContractTest {
    @Test
    void generatedMetadataDeclaresClientIntegrationsAndMixinConfig() throws IOException {
        String metadata = resource("META-INF/neoforge.mods.toml");

        assertTrue(metadata.contains("modId=\"emotecraft_fix_animate_texture\""));
        assertTrue(metadata.contains("modId=\"emotecraft\""));
        assertTrue(metadata.contains("modId=\"entity_model_features\""));
        assertTrue(metadata.contains("modId=\"playeranimator\""));
        assertTrue(metadata.contains("modId=\"bendylib\""));
        assertTrue(metadata.contains("modId=\"skinlayers3d\""));
        assertTrue(metadata.contains("config=\"emotecraft_fix_animate_texture.mixins.json\""));
        assertTrue(dependencyBlock(metadata, "emotecraft").contains("type=\"required\""));
        assertTrue(dependencyBlock(metadata, "entity_model_features").contains("type=\"optional\""));
        assertTrue(dependencyBlock(metadata, "playeranimator").contains("type=\"optional\""));
        assertTrue(dependencyBlock(metadata, "playeranimator").contains("versionRange=\"[2.0,)\""));
        assertTrue(dependencyBlock(metadata, "bendylib").contains("type=\"optional\""));
        assertTrue(dependencyBlock(metadata, "skinlayers3d").contains("type=\"optional\""));
        assertFalse(metadata.contains("modId=\"tacz\""));
        assertFalse(metadata.contains("${"));
    }

    @Test
    void mixinConfigIncludesAllOptionalCompatibilityHooks() throws IOException {
        String mixins = resource("emotecraft_fix_animate_texture.mixins.json");

        assertTrue(mixins.contains("EMFAnimationEntityContextMixin"));
        assertTrue(mixins.contains("PlayerModelLayerBendMixin"));
        assertTrue(mixins.contains("PlayerAnimatorAnimationApplierMixin"));
        assertTrue(mixins.contains("PlayerAnimatorBendHelperMixin"));
        assertTrue(mixins.contains("OptionalCompatMixinPlugin"));
    }

    private static String resource(String name) throws IOException {
        var resources = Collections.list(MetadataContractTest.class.getClassLoader().getResources(name));
        assertFalse(resources.isEmpty(), "Missing classpath resource " + name);

        StringBuilder combined = new StringBuilder();
        for (var resource : resources) {
            try (var stream = resource.openStream()) {
                combined.append(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).append('\n');
            }
        }
        return combined.toString();
    }

    private static String dependencyBlock(String metadata, String modId) {
        int start = metadata.indexOf("modId=\"" + modId + "\"");
        int end = metadata.indexOf("[[dependencies.", start);
        return metadata.substring(start, end < 0 ? metadata.length() : end);
    }
}
