package dev.tassis.emotecraftcompat.compat;

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
}
