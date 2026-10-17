package dev.tassis.emotecraftcompat.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.tr7zw.skinlayers.api.MeshTransformer;
import io.github.kosmx.bendylib.ICuboidBuilder;
import io.github.kosmx.bendylib.MutableCuboid;
import io.github.kosmx.bendylib.impl.BendableCuboid;
import io.github.kosmx.bendylib.impl.ICuboid;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinLayersFastRenderTest {
    @Test
    void appliesSourceBendToFastRenderWrapper() {
        ICuboidBuilder.Data cubeData = new ICuboidBuilder.Data(
                0, 0,
                -2.0F, -2.0F, -2.0F,
                4.0F, 12.0F, 4.0F,
                0.0F, 0.0F, 0.0F,
                false, 64.0F, 64.0F
        );
        BendableCuboid sourceBend = new BendableCuboid.Builder().setDirection(Direction.UP).build(cubeData);
        sourceBend.applyBend(0.35F, 0.8F);
        TestMutableCuboid cube = new TestMutableCuboid(cubeData);

        assertTrue(SkinLayersBendCompat.applyFastRenderBend(cube, sourceBend));

        ICuboid activeMutator = cube.getActiveMutator().getB();
        assertNotNull(activeMutator);
        assertFalse(activeMutator instanceof BendableCuboid);
        assertFalse(renderCuboid(activeMutator).isEmpty());
    }

    @Test
    void fastRenderCubeMatchesSlowPathAcrossLayerTransformSpaces() {
        ParityErrors identityError = fastToSlowParityError(1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F);
        ParityErrors bodyError = fastToSlowParityError(1.05F, 1.035F, 1.15F, 0.0F, -0.2F, 0.0F);
        ParityErrors standardArmError = fastToSlowParityError(1.15F, 1.035F, 1.15F, -0.998F, -0.1F, 0.0F);
        ParityErrors slimArmError = fastToSlowParityError(1.15F, 1.035F, 1.15F, -0.499F, -0.1F, 0.0F);

        assertTrue(identityError.position() < 0.00001F, "identity-space positions should agree");
        assertTrue(identityError.normal() < 0.00001F, "identity-space normals should agree");
        assertTrue(bodyError.position() < 0.00001F, "body-space positions should agree");
        assertTrue(bodyError.normal() < 0.00001F, "body-space normals should agree");
        assertTrue(standardArmError.position() < 0.00001F, "standard-arm positions should agree");
        assertTrue(standardArmError.normal() < 0.00001F, "standard-arm normals should agree");
        assertTrue(slimArmError.position() < 0.00001F, "slim-arm positions should agree");
        assertTrue(slimArmError.normal() < 0.00001F, "slim-arm normals should agree");
    }

    @Test
    void bendyFastCubeProvidesSubdividedBoundaryVertices() {
        ICuboidBuilder.Data cubeData = layerCubeData();
        ModelPart.Cube cube = createLayerCube();
        List<CapturedVertex> vanillaVertices = compileCube(cube);
        BendableCuboid sourceBend = new BendableCuboid.Builder().setDirection(Direction.UP).build(cubeData);
        sourceBend.applyBend(0.0F, 0.0F);
        TestMutableCuboid mutableCube = new TestMutableCuboid(cubeData);
        assertTrue(SkinLayersBendCompat.applyFastRenderBend(mutableCube, sourceBend));
        ICuboid fastCuboid = mutableCube.getActiveMutator().getB();
        List<CapturedVertex> bendyVertices = renderCuboid(fastCuboid);
        Set<Integer> distinctY = new HashSet<>();
        for (CapturedVertex vertex : bendyVertices) {
            distinctY.add(Math.round(vertex.position.y() * 16.0F));
        }

        assertTrue(bendyVertices.size() > vanillaVertices.size(), "BendyLib mutator should subdivide cube faces");
        for (int expectedY = -2; expectedY <= 10; expectedY++) {
            assertTrue(distinctY.contains(expectedY), "subdivision should retain pixel boundary y=" + expectedY);
        }

        sourceBend.applyBend(0.35F, 0.8F);
        assertTrue(SkinLayersBendCompat.applyFastRenderBend(mutableCube, sourceBend));
        List<CapturedVertex> bentVertices = renderCuboid(fastCuboid);
        assertEquals(bendyVertices.size(), bentVertices.size(), "non-zero bend must preserve subdivision topology");
        boolean internalBoundaryMoved = false;
        for (int index = 0; index < bendyVertices.size(); index++) {
            int originalY = Math.round(bendyVertices.get(index).position.y() * 16.0F);
            if (originalY > -2 && originalY < 10
                    && bendyVertices.get(index).position.distance(bentVertices.get(index).position) > 0.001F) {
                internalBoundaryMoved = true;
                break;
            }
        }
        assertTrue(internalBoundaryMoved, "bend must independently move vertices inside the subdivided span");
    }

    @Test
    void resetsOnlyTheEmotecraftFastRenderBend() {
        ICuboidBuilder.Data cubeData = new ICuboidBuilder.Data(
                0, 0,
                -2.0F, 0.0F, -2.0F,
                4.0F, 12.0F, 4.0F,
                0.0F, 0.0F, 0.0F,
                false, 64.0F, 64.0F
        );
        BendableCuboid sourceBend = new BendableCuboid.Builder().setDirection(Direction.UP).build(cubeData);
        sourceBend.applyBend(0.0F, 0.8F);
        TestMutableCuboid cube = new TestMutableCuboid(cubeData);

        assertTrue(SkinLayersBendCompat.applyFastRenderBend(cube, sourceBend));
        assertTrue(SkinLayersBendCompat.resetFastRenderBend(cube));
        assertNull(cube.getActiveMutator());

        cube.registerMutator("other_mod", data -> new BendableCuboid.Builder()
                .setDirection(Direction.UP)
                .build(data));
        cube.getAndActivateMutator("other_mod");
        assertFalse(SkinLayersBendCompat.resetFastRenderBend(cube));
        assertEquals("other_mod", cube.getActiveMutator().getA());
    }

    private static ParityErrors fastToSlowParityError(
            float xScale,
            float yScale,
            float zScale,
            float xOffset,
            float yOffset,
            float zOffset
    ) {
        ICuboidBuilder.Data cubeData = layerCubeData();
        BendableCuboid sourceBend = new BendableCuboid.Builder().setDirection(Direction.UP).build(cubeData);
        sourceBend.applyBend(0.35F, 0.0F);
        TestMutableCuboid mutableCube = new TestMutableCuboid(cubeData);
        assertTrue(SkinLayersBendCompat.applyFastRenderBend(
                mutableCube, sourceBend, xScale, yScale, zScale, xOffset, yOffset, zOffset
        ));
        ICuboid fastCubeBend = mutableCube.getActiveMutator().getB();
        List<CapturedVertex> baseline = renderCuboid(fastCubeBend);

        sourceBend.applyBend(0.35F, 0.8F);
        assertTrue(SkinLayersBendCompat.applyFastRenderBend(
                mutableCube, sourceBend, xScale, yScale, zScale, xOffset, yOffset, zOffset
        ));
        List<CapturedVertex> actual = renderCuboid(fastCubeBend);
        assertTrue(SkinLayersBendCompat.applyFastRenderBend(
                mutableCube, sourceBend, xScale, yScale, zScale, xOffset, yOffset, zOffset
        ));
        List<CapturedVertex> repeated = renderCuboid(fastCubeBend);
        assertEquals(baseline.size(), actual.size(), "bend must keep the subdivided topology stable");
        assertEquals(actual.size(), repeated.size(), "reapplying a bend must keep the topology stable");
        assertEquals(0, baseline.size() % 4, "captured cube output must contain complete quads");

        MeshTransformer slowTransformer = SkinLayersBendCompat.createTransformer(
                sourceBend, xScale, yScale, zScale, xOffset, yOffset, zOffset
        );
        float maxPositionError = 0.0F;
        float maxNormalError = 0.0F;
        for (int start = 0; start < baseline.size(); start += 4) {
            Vector4f[] expectedVertices = new Vector4f[4];
            for (int offset = 0; offset < 4; offset++) {
                Vector3f position = baseline.get(start + offset).position;
                expectedVertices[offset] = new Vector4f(position.x(), position.y(), position.z(), 1.0F);
            }
            Vector3f expectedNormal = new Vector3f(baseline.get(start).normal);
            slowTransformer.transform(expectedNormal, expectedVertices);

            for (int offset = 0; offset < 4; offset++) {
                CapturedVertex actualVertex = actual.get(start + offset);
                maxPositionError = Math.max(maxPositionError, new Vector3f(
                        expectedVertices[offset].x(),
                        expectedVertices[offset].y(),
                        expectedVertices[offset].z()
                ).distance(actualVertex.position));
                maxNormalError = Math.max(maxNormalError, expectedNormal.distance(actualVertex.normal));
                assertTrue(
                        actualVertex.position.distance(repeated.get(start + offset).position) < 0.000001F,
                        "reapplying the same bend must not accumulate position drift"
                );
                assertTrue(
                        actualVertex.normal.distance(repeated.get(start + offset).normal) < 0.000001F,
                        "reapplying the same bend must not accumulate normal drift"
                );
            }
        }
        return new ParityErrors(maxPositionError, maxNormalError);
    }

    private static ICuboidBuilder.Data layerCubeData() {
        return new ICuboidBuilder.Data(
                0, 0,
                -2.0F, -2.0F, -2.0F,
                4.0F, 12.0F, 4.0F,
                0.0F, 0.0F, 0.0F,
                false, 64.0F, 64.0F
        );
    }

    private static ModelPart.Cube createLayerCube() {
        return CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F)
                .getCubes()
                .get(0)
                .bake(64, 64);
    }

    private static List<CapturedVertex> compileCube(ModelPart.Cube cube) {
        PoseStack poseStack = new PoseStack();
        return captureVertices(consumer -> cube.compile(poseStack.last(), consumer, 0, 0, 0xFFFFFFFF));
    }

    private static List<CapturedVertex> renderCuboid(ICuboid cuboid) {
        PoseStack poseStack = new PoseStack();
        return captureVertices(consumer -> cuboid.render(poseStack.last(), consumer, 0, 0, 0xFFFFFFFF));
    }

    private static List<CapturedVertex> captureVertices(Consumer<VertexConsumer> render) {
        List<CapturedVertex> vertices = new ArrayList<>();
        VertexConsumer consumer = (VertexConsumer) Proxy.newProxyInstance(
                VertexConsumer.class.getClassLoader(),
                new Class<?>[] {VertexConsumer.class},
                (proxy, method, arguments) -> {
                    if ("addVertex".equals(method.getName()) && arguments != null && arguments.length >= 3) {
                        CapturedVertex vertex = new CapturedVertex(new Vector3f(
                                ((Number) arguments[0]).floatValue(),
                                ((Number) arguments[1]).floatValue(),
                                ((Number) arguments[2]).floatValue()
                        ));
                        if (arguments.length >= 11) {
                            vertex.normal.set(
                                    ((Number) arguments[8]).floatValue(),
                                    ((Number) arguments[9]).floatValue(),
                                    ((Number) arguments[10]).floatValue()
                            );
                        }
                        vertices.add(vertex);
                    } else if ("setNormal".equals(method.getName())
                            && arguments != null
                            && arguments.length >= 3
                            && !vertices.isEmpty()) {
                        vertices.get(vertices.size() - 1).normal.set(
                                ((Number) arguments[0]).floatValue(),
                                ((Number) arguments[1]).floatValue(),
                                ((Number) arguments[2]).floatValue()
                        );
                    }
                    if (method.getReturnType().isInstance(proxy)) {
                        return proxy;
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType().isPrimitive()) {
                        return 0;
                    }
                    return null;
                }
        );
        render.accept(consumer);
        return vertices;
    }

    private record ParityErrors(float position, float normal) {
    }

    private static final class CapturedVertex {
        private final Vector3f position;
        private final Vector3f normal = new Vector3f();

        private CapturedVertex(Vector3f position) {
            this.position = position;
        }
    }

    private static final class TestMutableCuboid implements MutableCuboid {
        private final ICuboidBuilder.Data data;
        private final Map<String, ICuboidBuilder<ICuboid>> builders = new HashMap<>();
        private final Map<String, ICuboid> mutators = new HashMap<>();
        private String activeId;

        private TestMutableCuboid(ICuboidBuilder.Data data) {
            this.data = data;
        }

        @Override
        public boolean registerMutator(String id, ICuboidBuilder<ICuboid> builder) {
            return builders.putIfAbsent(id, builder) == null;
        }

        @Override
        public boolean unregisterMutator(String id) {
            mutators.remove(id);
            return builders.remove(id) != null;
        }

        @Override
        public Tuple<String, ICuboid> getActiveMutator() {
            return activeId == null ? null : new Tuple<>(activeId, mutators.get(activeId));
        }

        @Override
        public boolean hasMutator(String id) {
            return builders.containsKey(id);
        }

        @Override
        public ICuboidBuilder<ICuboid> getCuboidBuilder(String id) {
            return builders.get(id);
        }

        @Override
        public ICuboid getMutator(String id) {
            return mutators.get(id);
        }

        @Override
        public ICuboid getAndActivateMutator(String id) {
            if (id == null) {
                activeId = null;
                return null;
            }
            ICuboidBuilder<ICuboid> builder = builders.get(id);
            if (builder == null) {
                return null;
            }
            activeId = id;
            return mutators.computeIfAbsent(id, ignored -> builder.build(data));
        }

        @Override
        public void copyStateFrom(MutableCuboid source) {
            throw new UnsupportedOperationException();
        }
    }
}
