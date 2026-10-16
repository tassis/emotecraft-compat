package dev.tassis.emotecraftcompat.compat;

import dev.tassis.emotecraftcompat.EmotecraftCompatMod;
import dev.tassis.emotecraftcompat.state.EmoteStateManager;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.impl.IUpperPartHelper;
import dev.kosmx.playerAnim.impl.animation.AnimationApplier;
import dev.kosmx.playerAnim.impl.animation.IBendHelper;
import dev.tr7zw.skinlayers.api.LayerFeatureTransformerAPI;
import dev.tr7zw.skinlayers.api.MeshTransformer;
import dev.tr7zw.skinlayers.api.MeshTransformerProvider;
import dev.tr7zw.skinlayers.api.SkinLayersAPI;
import dev.tr7zw.skinlayers.SkinLayersModBase;
import io.github.kosmx.bendylib.MutableCuboid;
import io.github.kosmx.bendylib.ModelPartAccessor;
import io.github.kosmx.bendylib.impl.BendableCuboid;
import io.github.kosmx.bendylib.impl.RememberingPos;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Tuple;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class SkinLayersBendCompat {
    private static final float MODEL_UNIT_SCALE = 16.0F;
    private static final float LAYER_HEIGHT_SCALE = 1.035F;
    private static final float NORMAL_EPSILON = 0.000001F;
    private static final String FAST_RENDER_BEND_MUTATOR = "emotecraft_3d_skin_layer_bend";
    private static final MeshTransformer RESET_FAST_RENDER_BEND = new MeshTransformer() {
        @Override
        public void transform(Vector3f normal, Vector4f[] vertices) {
        }

        @Override
        public void transform(ModelPart.Cube cube) {
            resetFastRenderBend((MutableCuboid) (Object) cube);
        }
    };
    private static final ThreadLocal<Boolean> EMOTE_RENDER_SCOPE = new ThreadLocal<>();
    private static boolean missingBendLogged;

    private SkinLayersBendCompat() {
    }

    public static void register() {
        try {
            if (SkinLayersAPI.getMeshTransformerProvider() != MeshTransformerProvider.EMPTY_PROVIDER) {
                EmotecraftCompatMod.LOGGER.info(
                        "3D Skin Layers already has a mesh transformer provider; keeping the existing integration"
                );
                return;
            }
            SkinLayersAPI.setupMeshTransformerProvider(SkinLayersBendCompat::prepareTransformer);
            LayerFeatureTransformerAPI.LayerTransformer existingTransformer =
                    LayerFeatureTransformerAPI.getTransformer();
            LayerFeatureTransformerAPI.setLayerTransformer((player, poseStack, modelPart) -> {
                existingTransformer.transform(player, poseStack, modelPart);
                boolean emoteActive = EmoteStateManager.isEmoteActive(player.getUUID());
                EMOTE_RENDER_SCOPE.set(emoteActive);
                if (emoteActive
                        && ((IUpperPartHelper) (Object) modelPart).isUpperPart()
                        && player instanceof IAnimatedPlayer animatedPlayer) {
                    AnimationApplier animation = animatedPlayer.playerAnimator_getAnimation();
                    if (animation.isActive()) {
                        IBendHelper.rotateMatrixStack(poseStack, animation.getBend("body"));
                    }
                }
            });
            EmotecraftCompatMod.LOGGER.info("Registered Player Animator bend support for 3D Skin Layers");
        } catch (LinkageError | RuntimeException exception) {
            EmotecraftCompatMod.LOGGER.warn("Failed to register 3D Skin Layers bend compatibility", exception);
        }
    }

    static MeshTransformer prepareTransformer(ModelPart modelPart) {
        PlayerAnimatorBendCompat.LayerSource layerSource = PlayerAnimatorBendCompat.getLayerSource(modelPart);
        boolean emoteActive = Boolean.TRUE.equals(EMOTE_RENDER_SCOPE.get()) || layerSource != null;
        EMOTE_RENDER_SCOPE.remove();
        if (modelPart == null) {
            return MeshTransformer.EMPTY_TRANSFORMER;
        }
        if (!emoteActive) {
            return RESET_FAST_RENDER_BEND;
        }

        try {
            BendableCuboid bend = findActiveBend(modelPart);
            if (bend == null && layerSource != null) {
                bend = findActiveBend(layerSource.modelPart());
            }
            if (bend == null) {
                if (!missingBendLogged) {
                    missingBendLogged = true;
                    EmotecraftCompatMod.LOGGER.warn(
                            "3D Skin Layers reached an active Emotecraft render without an active BendyLib mutator"
                    );
                }
                return RESET_FAST_RENDER_BEND;
            }
            float armOffset = layerSource == null ? 0.0F : layerSource.armOffset();
            return createTransformer(modelPart, bend, armOffset);
        } catch (RuntimeException exception) {
            if (!missingBendLogged) {
                missingBendLogged = true;
                EmotecraftCompatMod.LOGGER.warn(
                        "3D Skin Layers could not read the active BendyLib mutator",
                        exception
                );
            }
            return RESET_FAST_RENDER_BEND;
        }
    }

    private static BendableCuboid findActiveBend(ModelPart modelPart) {
        return ModelPartAccessor.optionalGetCuboid(modelPart, 0)
                .map(cuboid -> cuboid.getActiveMutator())
                .map(Tuple::getB)
                .filter(BendableCuboid.class::isInstance)
                .map(BendableCuboid.class::cast)
                .orElse(null);
    }

    private static MeshTransformer createTransformer(
            ModelPart modelPart,
            BendableCuboid bendableCuboid,
            float registeredArmOffset
    ) {
        float xOffset = 0.0F;
        float yOffset = -0.2F;
        boolean bodyPart = bendableCuboid.getBendDirection() == net.minecraft.core.Direction.DOWN;
        boolean armPart = !bodyPart && ((IUpperPartHelper) (Object) modelPart).isUpperPart();

        if (armPart) {
            yOffset = -0.1F;
            if (registeredArmOffset != 0.0F) {
                xOffset = registeredArmOffset;
            } else {
                float width = Math.abs(bendableCuboid.maxX - bendableCuboid.minX);
                float armOffset = width < 3.5F ? 0.499F : 0.998F;
                xOffset = modelPart.x < 0.0F ? -armOffset : armOffset;
            }
        }

        float widthScale = bodyPart
                ? SkinLayersModBase.config.bodyVoxelWidthSize
                : SkinLayersModBase.config.baseVoxelSize;
        float depthScale = SkinLayersModBase.config.baseVoxelSize;

        return new SkinLayersBendTransformer(
                bendableCuboid,
                new TransformSpace(
                        widthScale,
                        LAYER_HEIGHT_SCALE,
                        depthScale,
                        xOffset,
                        yOffset,
                        0.0F
                )
        );
    }

    static MeshTransformer createTransformer(
            BendableCuboid bendableCuboid,
            float xScale,
            float yScale,
            float zScale,
            float xOffset,
            float yOffset,
            float zOffset
    ) {
        return new SkinLayersBendTransformer(
                bendableCuboid,
                new TransformSpace(xScale, yScale, zScale, xOffset, yOffset, zOffset)
        );
    }

    private static final class SkinLayersBendTransformer implements MeshTransformer {
        private final BendableCuboid bendableCuboid;
        private final TransformSpace transformSpace;

        private SkinLayersBendTransformer(BendableCuboid bendableCuboid, TransformSpace transformSpace) {
            this.bendableCuboid = bendableCuboid;
            this.transformSpace = transformSpace;
        }

        @Override
        public void transform(Vector3f normal, Vector4f[] vertices) {
            RememberingPos[] positions = new RememberingPos[vertices.length];
            for (int index = 0; index < vertices.length; index++) {
                Vector4f vertex = vertices[index];
                positions[index] = new RememberingPos(
                        transformSpace.toVanillaX(vertex.x() * MODEL_UNIT_SCALE),
                        transformSpace.toVanillaY(vertex.y() * MODEL_UNIT_SCALE),
                        transformSpace.toVanillaZ(vertex.z() * MODEL_UNIT_SCALE)
                );
            }

            bendableCuboid.applyBend(
                    bendableCuboid.getBendAxis(),
                    bendableCuboid.getBend(),
                    consumer -> {
                        for (RememberingPos position : positions) {
                            consumer.accept(position);
                        }
                    }
            );

            for (int index = 0; index < vertices.length; index++) {
                Vector3f position = positions[index].getPos();
                vertices[index].set(
                        transformSpace.fromVanillaX(position.x()) / MODEL_UNIT_SCALE,
                        transformSpace.fromVanillaY(position.y()) / MODEL_UNIT_SCALE,
                        transformSpace.fromVanillaZ(position.z()) / MODEL_UNIT_SCALE,
                        1.0F
                );
            }
            updateNormal(normal, vertices);
        }

        @Override
        public void transform(ModelPart.Cube cube) {
            applyFastRenderBend((MutableCuboid) (Object) cube, bendableCuboid);
        }
    }

    static boolean applyFastRenderBend(MutableCuboid mutableCube, BendableCuboid sourceBend) {
        if (!mutableCube.hasMutator(FAST_RENDER_BEND_MUTATOR)) {
            mutableCube.registerMutator(
                    FAST_RENDER_BEND_MUTATOR,
                    data -> new BendableCuboid.Builder()
                            .setDirection(sourceBend.getBendDirection())
                            .build(data)
            );
        }

        if (!(mutableCube.getAndActivateMutator(FAST_RENDER_BEND_MUTATOR) instanceof BendableCuboid cubeBend)) {
            return false;
        }
        cubeBend.applyBend(sourceBend.getBendAxis(), sourceBend.getBend());
        return true;
    }

    static boolean resetFastRenderBend(MutableCuboid mutableCube) {
        Tuple<String, ?> activeMutator = mutableCube.getActiveMutator();
        if (activeMutator == null || !FAST_RENDER_BEND_MUTATOR.equals(activeMutator.getA())) {
            return false;
        }
        mutableCube.getAndActivateMutator(null);
        return true;
    }

    private static void updateNormal(Vector3f normal, Vector4f[] vertices) {
        Vector3f originalNormal = new Vector3f(normal);
        Vector3f firstEdge = subtract(vertices[1], vertices[0]);
        Vector3f secondEdge = subtract(vertices[2], vertices[0]);
        Vector3f transformedNormal = firstEdge.cross(secondEdge);

        if (transformedNormal.lengthSquared() < NORMAL_EPSILON && vertices.length > 3) {
            firstEdge = subtract(vertices[2], vertices[0]);
            secondEdge = subtract(vertices[3], vertices[0]);
            transformedNormal = firstEdge.cross(secondEdge);
        }
        if (transformedNormal.lengthSquared() < NORMAL_EPSILON) {
            return;
        }

        transformedNormal.normalize();
        if (transformedNormal.dot(originalNormal) < 0.0F) {
            transformedNormal.negate();
        }
        normal.set(transformedNormal);
    }

    private static Vector3f subtract(Vector4f left, Vector4f right) {
        return new Vector3f(left.x() - right.x(), left.y() - right.y(), left.z() - right.z());
    }

    private record TransformSpace(
            float xScale,
            float yScale,
            float zScale,
            float xOffset,
            float yOffset,
            float zOffset
    ) {
        private float toVanillaX(float value) {
            return (value + xOffset) * xScale;
        }

        private float toVanillaY(float value) {
            return (value + yOffset) * yScale;
        }

        private float toVanillaZ(float value) {
            return (value + zOffset) * zScale;
        }

        private float fromVanillaX(float value) {
            return value / xScale - xOffset;
        }

        private float fromVanillaY(float value) {
            return value / yScale - yOffset;
        }

        private float fromVanillaZ(float value) {
            return value / zScale - zOffset;
        }
    }

}
