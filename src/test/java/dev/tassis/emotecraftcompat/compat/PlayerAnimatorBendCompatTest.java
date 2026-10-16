package dev.tassis.emotecraftcompat.compat;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerAnimatorBendCompatTest {
    @Test
    void assignsBendDirectionsToPlayerOuterLayers() {
        assertEquals(Direction.DOWN, PlayerAnimatorBendCompat.bendDirection("jacket"));
        assertEquals(Direction.UP, PlayerAnimatorBendCompat.bendDirection("rightSleeve"));
        assertEquals(Direction.UP, PlayerAnimatorBendCompat.bendDirection("leftSleeve"));
        assertEquals(Direction.UP, PlayerAnimatorBendCompat.bendDirection("rightPants"));
        assertEquals(Direction.UP, PlayerAnimatorBendCompat.bendDirection("leftPants"));
    }
}
