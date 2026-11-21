package dev.tassis.emotecraftcompat.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class OptionalCompatMixinPlugin implements IMixinConfigPlugin {
    private static final String PLAYER_ANIMATOR_MOD_ID = "playeranimator";
    private static final String BENDYLIB_MOD_ID = "bendylib";

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
                OptionalCompatMixinPlugin::isModLoaded
        );
    }

    public static boolean shouldApplyMixin(String mixinClassName, Predicate<String> isLoaded) {
        if (mixinClassName.endsWith("PlayerAnimatorAnimationApplierMixin")
                || mixinClassName.endsWith("PlayerAnimatorBendHelperMixin")
                || mixinClassName.endsWith("PlayerModelLayerBendMixin")) {
            return isLoaded.test(PLAYER_ANIMATOR_MOD_ID) && isLoaded.test(BENDYLIB_MOD_ID);
        }
        return true;
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

}
