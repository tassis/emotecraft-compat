package dev.tassis.emotecraftcompat.compat;

import dev.tassis.emotecraftcompat.EmotecraftCompatMod;
import io.github.kosmx.bendylib.ModelPartAccessor;
import io.github.kosmx.bendylib.impl.BendableCuboid;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerAnimatorBendCompat {
    private static final Set<String> REPAIR_LOGGED_PARTS = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED_LOGGED_PARTS = ConcurrentHashMap.newKeySet();

    private PlayerAnimatorBendCompat() {
    }

    public static void ensureBendMutator(String partName, ModelPart modelPart) {
        Direction direction = bendDirection(partName);
        if (direction == null || modelPart == null) {
            return;
        }

        try {
            ModelPartAccessor.optionalGetCuboid(modelPart, 0).ifPresent(mutableCuboid -> {
                if (mutableCuboid.hasMutator("bend")) {
                    return;
                }

                boolean registered = mutableCuboid.registerMutator(
                        "bend",
                        data -> new BendableCuboid.Builder().setDirection(direction).build(data)
                );
                if (!registered || !mutableCuboid.hasMutator("bend")) {
                    logRepairFailure(partName);
                    return;
                }
                if (REPAIR_LOGGED_PARTS.add(normalize(partName))) {
                    EmotecraftCompatMod.LOGGER.info(
                            "Re-registered missing Player Animator bend mutator for {} with direction {}",
                            partName,
                            direction
                    );
                }
            });
        } catch (RuntimeException exception) {
            logRepairFailure(partName);
        }
    }

    static Direction bendDirection(String partName) {
        return switch (normalize(partName)) {
            case "torso", "body", "jacket" -> Direction.DOWN;
            case "leftarm", "rightarm", "leftleg", "rightleg",
                    "leftsleeve", "rightsleeve", "leftpants", "rightpants" -> Direction.UP;
            default -> null;
        };
    }

    private static String normalize(String partName) {
        return partName == null ? "" : partName.replace("_", "").toLowerCase(Locale.ROOT);
    }

    private static void logRepairFailure(String partName) {
        if (FAILED_LOGGED_PARTS.add(normalize(partName))) {
            EmotecraftCompatMod.LOGGER.warn(
                    "Could not safely re-register the Player Animator bend mutator for {}",
                    partName
            );
        }
    }
}
