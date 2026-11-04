package dev.tassis.emotecraftcompat;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

import java.util.function.Predicate;

@Mod(EmotecraftCompatMod.MOD_ID)
public final class EmotecraftCompatMod {
    public static final String MOD_ID = "emotecraft_compat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EmotecraftCompatMod() {
        LOGGER.info("Initializing {}", MOD_ID);
    }

    public static boolean canRegisterSkinLayersBendCompat(Predicate<String> isLoaded) {
        return isLoaded.test("skinlayers3d")
                && isLoaded.test("playeranimator")
                && isLoaded.test("bendylib");
    }

}
