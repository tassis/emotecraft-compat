package dev.tassis.emotecraftcompat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EmotecraftCompatMod.MOD_ID)
public final class EmotecraftCompatMod {
    public static final String MOD_ID = "emotecraft_compat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EmotecraftCompatMod() {
        LOGGER.info("Initializing {}", MOD_ID);
    }
}
