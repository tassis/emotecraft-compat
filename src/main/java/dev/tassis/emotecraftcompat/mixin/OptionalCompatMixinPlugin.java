package dev.tassis.emotecraftcompat.mixin;

import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class OptionalCompatMixinPlugin implements IMixinConfigPlugin {
    private static final String EMF_MOD_ID = "entity_model_features";
    private static final String EMOTECRAFT_MOD_ID = "emotecraft";
    private static final String PLAYER_ANIMATOR_MOD_ID = "playeranimator";
    private static final String BENDYLIB_MOD_ID = "bendylib";
    private static final String AFFECTED_EMOTECRAFT_VERSION = "2.4.12";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return shouldApplyMixin(
                mixinClassName,
                OptionalCompatMixinPlugin::isModLoaded,
                OptionalCompatMixinPlugin::getModVersion
        );
    }

    public static boolean shouldApplyMixin(String mixinClassName, Predicate<String> isLoaded) {
        return shouldApplyMixin(mixinClassName, isLoaded, ignored -> AFFECTED_EMOTECRAFT_VERSION);
    }

    public static boolean shouldApplyMixin(
            String mixinClassName,
            Predicate<String> isLoaded,
            Function<String, String> versionLookup
    ) {
        if (mixinClassName.endsWith("EmotecraftConfigurationSyncMixin")) {
            return isLoaded.test(EMOTECRAFT_MOD_ID)
                    && isAffectedEmotecraftVersion(versionLookup.apply(EMOTECRAFT_MOD_ID));
        }
        if (mixinClassName.endsWith("EMFAnimationEntityContextMixin")) {
            return isLoaded.test(EMF_MOD_ID);
        }
        if (mixinClassName.endsWith("PlayerModelLayerBendMixin")
                || mixinClassName.endsWith("PlayerAnimatorAnimationApplierMixin")
                || mixinClassName.endsWith("PlayerAnimatorBendHelperMixin")) {
            return isLoaded.test(PLAYER_ANIMATOR_MOD_ID) && isLoaded.test(BENDYLIB_MOD_ID);
        }
        return true;
    }

    static boolean isAffectedEmotecraftVersion(String version) {
        return version != null && (version.equals(AFFECTED_EMOTECRAFT_VERSION)
                || version.startsWith(AFFECTED_EMOTECRAFT_VERSION + "+")
                || version.startsWith(AFFECTED_EMOTECRAFT_VERSION + "-"));
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isModLoaded(String modId) {
        return FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }

    private static String getModVersion(String modId) {
        var modFile = FMLLoader.getLoadingModList().getModFileById(modId);
        if (modFile == null) {
            return null;
        }
        return modFile.getMods().stream()
                .filter(mod -> modId.equals(mod.getModId()))
                .map(mod -> mod.getVersion().toString())
                .findFirst()
                .orElse(null);
    }
}
