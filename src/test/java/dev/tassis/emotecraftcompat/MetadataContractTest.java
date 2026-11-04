package dev.tassis.emotecraftcompat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataContractTest {
    @Test
    void generatedMetadataDeclaresIndependentClientAndServerIntegrations() throws IOException {
        String metadata = resource("META-INF/neoforge.mods.toml");

        assertTrue(metadata.contains("license=\"MIT\""));
        assertTrue(metadata.contains("modId=\"emotecraft_compat\""));
        assertTrue(metadata.contains("modId=\"emotecraft\""));
        assertTrue(metadata.contains("modId=\"entity_model_features\""));
        assertTrue(metadata.contains("modId=\"playeranimator\""));
        assertTrue(metadata.contains("modId=\"bendylib\""));
        assertTrue(metadata.contains("modId=\"skinlayers3d\""));
        assertTrue(metadata.contains("config=\"emotecraft_compat.mixins.json\""));
        assertTrue(dependencyBlock(metadata, "neoforge").contains("side=\"BOTH\""));
        assertTrue(dependencyBlock(metadata, "minecraft").contains("side=\"BOTH\""));
        assertTrue(metadata.contains("""
                modId="emotecraft"
                type="required"
                versionRange="[0,)"
                ordering="AFTER"
                side="CLIENT"
                """));
        assertTrue(metadata.contains("""
                modId="emotecraft"
                type="optional"
                versionRange="[0,)"
                ordering="AFTER"
                side="SERVER"
                """));
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
        String mixins = resource("emotecraft_compat.mixins.json");

        assertTrue(mixins.contains("EMFAnimationEntityContextMixin"));
        assertTrue(mixins.contains("EmotecraftConfigurationSyncMixin"));
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
        return dependencyBlocks(metadata, modId).stream().findFirst().orElse("");
    }

    private static List<String> dependencyBlocks(String metadata, String modId) {
        return List.of(metadata.split("(?=\\[\\[dependencies\\.)")).stream()
                .filter(block -> block.startsWith("[[dependencies.emotecraft_compat]]"))
                .filter(block -> block.contains("modId=\"" + modId + "\""))
                .distinct()
                .toList();
    }
}
