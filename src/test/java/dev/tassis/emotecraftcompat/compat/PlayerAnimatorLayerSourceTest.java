package dev.tassis.emotecraftcompat.compat;

import dev.tassis.emotecraftcompat.state.EmoteStateManager;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PlayerAnimatorLayerSourceTest {
    @Test
    void tracksLayerSourceOnlyWhileEmoteIsActive() {
        ModelPart outerPart = new ModelPart(List.of(), Map.of());
        ModelPart sourcePart = new ModelPart(List.of(), Map.of());

        PlayerAnimatorBendCompat.setLayerSource(outerPart, sourcePart, 0.0F);
        PlayerAnimatorBendCompat.LayerSource source = PlayerAnimatorBendCompat.getLayerSource(outerPart);
        assertSame(sourcePart, source.modelPart());
        assertEquals(0.0F, source.armOffset());

        PlayerAnimatorBendCompat.removeLayerSource(outerPart);
        assertNull(PlayerAnimatorBendCompat.getLayerSource(outerPart));
    }

    @Test
    void keepsRegisteredSleeveOffsetWhenArmCrossesTheBody() {
        ModelPart outerPart = new ModelPart(List.of(), Map.of());
        ModelPart sourcePart = new ModelPart(List.of(), Map.of());
        sourcePart.x = 8.0F;

        PlayerAnimatorBendCompat.setLayerSource(outerPart, sourcePart, -0.499F);

        assertEquals(-0.499F, PlayerAnimatorBendCompat.getLayerSource(outerPart).armOffset());

        PlayerAnimatorBendCompat.removeLayerSource(outerPart);
    }

    @Test
    void clearsAllLayerSourcesAndAllowsFreshRegistration() {
        ModelPart firstOuterPart = new ModelPart(List.of(), Map.of());
        ModelPart secondOuterPart = new ModelPart(List.of(), Map.of());
        ModelPart firstSourcePart = new ModelPart(List.of(), Map.of());
        ModelPart secondSourcePart = new ModelPart(List.of(), Map.of());
        PlayerAnimatorBendCompat.setLayerSource(firstOuterPart, firstSourcePart, 0.0F);
        PlayerAnimatorBendCompat.setLayerSource(secondOuterPart, secondSourcePart, 0.499F);
        assertEquals(2, PlayerAnimatorBendCompat.layerSourceCount());

        EmoteStateManager.clear();
        PlayerAnimatorBendCompat.clearLayerSources();

        assertEquals(0, PlayerAnimatorBendCompat.layerSourceCount());
        assertNull(PlayerAnimatorBendCompat.getLayerSource(firstOuterPart));
        assertNull(PlayerAnimatorBendCompat.getLayerSource(secondOuterPart));

        PlayerAnimatorBendCompat.setLayerSource(firstOuterPart, secondSourcePart, -0.499F);
        assertEquals(1, PlayerAnimatorBendCompat.layerSourceCount());
        assertSame(secondSourcePart, PlayerAnimatorBendCompat.getLayerSource(firstOuterPart).modelPart());
        PlayerAnimatorBendCompat.clearLayerSources();
    }
}
