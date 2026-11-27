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
    void generatedMetadataDeclaresForgeClientIntegrations() throws IOException {
        String metadata = resource("META-INF/mods.toml");

        assertTrue(metadata.contains("license=\"MIT\""));
        assertTrue(metadata.contains("modId=\"emotecraft_compat\""));
        assertTrue(metadata.contains("modId=\"emotecraft\""));
        assertTrue(metadata.contains("modId=\"entity_model_features\""));
        assertTrue(metadata.contains("modId=\"playeranimator\""));
        assertTrue(metadata.contains("modId=\"bendylib\""));
        assertTrue(dependencyBlock(metadata, "forge").contains("side=\"BOTH\""));
        assertTrue(dependencyBlock(metadata, "forge").contains("versionRange=\"[47.2.0,48)\""));
        assertTrue(dependencyBlock(metadata, "minecraft").contains("side=\"BOTH\""));
        assertTrue(dependencyBlock(metadata, "minecraft").contains("versionRange=\"[1.20.1]\""));
        assertTrue(metadata.contains("""
                modId="emotecraft"
                mandatory=true
                versionRange="[2.2.7-b.build.50]"
                ordering="AFTER"
                side="CLIENT"
                """));
        assertTrue(dependencyBlock(metadata, "entity_model_features").contains("mandatory=false"));
        assertTrue(dependencyBlock(metadata, "entity_model_features").contains("versionRange=\"[3.3.12]\""));
        assertTrue(dependencyBlock(metadata, "playeranimator").contains("mandatory=false"));
        assertTrue(dependencyBlock(metadata, "playeranimator").contains("versionRange=\"[1.0.2-rc1+1.20]\""));
        assertTrue(dependencyBlock(metadata, "bendylib").contains("mandatory=false"));
        assertTrue(dependencyBlock(metadata, "bendylib").contains("versionRange=\"[4.0.0]\""));
        assertFalse(metadata.contains("modId=\"skinlayers3d\""));
        assertFalse(metadata.contains("${"));
    }

    @Test
    void mixinConfigIncludesForgeCompatibilityHooks() throws IOException {
        String mixins = resource("emotecraft_compat.mixins.json");

        assertTrue(mixins.contains("PlayerAnimatorAnimationApplierMixin"));
        assertTrue(mixins.contains("PlayerAnimatorBendHelperMixin"));
        assertTrue(mixins.contains("PlayerModelLayerBendMixin"));
        assertTrue(mixins.contains("OptionalCompatMixinPlugin"));
        assertTrue(mixins.contains("\"refmap\": \"emotecraft_compat.refmap.json\""));
        assertFalse(mixins.contains("EmotecraftConfigurationSyncMixin"));
        assertFalse(mixins.contains("EMFAnimationEntityContextMixin"));
    }

    private static String resource(String name) throws IOException {
        var resources = Collections.list(MetadataContractTest.class.getClassLoader().getResources(name));
        assertFalse(resources.isEmpty(), "Missing classpath resource " + name);

        StringBuilder combined = new StringBuilder();
        for (var resource : resources) {
            try (var stream = resource.openStream()) {
                String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                if (content.contains("modId=\"emotecraft_compat\"")) {
                    return content;
                }
                combined.append(content).append('\n');
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
