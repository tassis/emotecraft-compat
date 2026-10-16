package dev.tassis.emotecraftcompat.compat;

import dev.tr7zw.skinlayers.api.MeshTransformer;
import io.github.kosmx.bendylib.ICuboidBuilder;
import io.github.kosmx.bendylib.MutableCuboid;
import io.github.kosmx.bendylib.impl.BendableCuboid;
import io.github.kosmx.bendylib.impl.ICuboid;
import io.github.kosmx.bendylib.impl.RememberingPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinLayersBendCompatTest {
    @Test
    void transformsSkinLayerVerticesWithActiveBendyMutator() {
        ICuboidBuilder.Data legData = new ICuboidBuilder.Data(
                0, 0,
                -2.0F, 0.0F, -2.0F,
                4.0F, 12.0F, 4.0F,
                0.0F, 0.0F, 0.0F,
                false, 64.0F, 64.0F
        );
        BendableCuboid bend = new BendableCuboid.Builder().setDirection(Direction.UP).build(legData);
        bend.applyBend(0.0F, 0.75F);
        MeshTransformer transformer = SkinLayersBendCompat.createTransformer(
                bend,
                1.0F,
                1.0F,
                1.0F,
                0.0F,
                0.0F,
                0.0F
        );

        Vector3f normal = new Vector3f(0.0F, 0.0F, 1.0F);
        Vector4f[] vertices = {
                new Vector4f(-0.125F, 0.75F, 0.125F, 1.0F),
                new Vector4f(0.125F, 0.75F, 0.125F, 1.0F),
                new Vector4f(0.125F, 0.5F, 0.125F, 1.0F),
                new Vector4f(-0.125F, 0.5F, 0.125F, 1.0F)
        };
        float originalY = vertices[0].y();
        float originalZ = vertices[0].z();

        transformer.transform(normal, vertices);

        assertTrue(Float.isFinite(vertices[0].x()));
        assertTrue(Float.isFinite(vertices[0].y()));
        assertTrue(Float.isFinite(vertices[0].z()));
        assertTrue(Float.isFinite(normal.x()));
        assertTrue(Float.isFinite(normal.y()));
        assertTrue(Float.isFinite(normal.z()));
        assertNotEquals(originalY, vertices[0].y());
        assertNotEquals(originalZ, vertices[0].z());
    }

    @Test
    void alignsScaledArmMeshBeforeApplyingVanillaBend() {
        BendableCuboid bend = createArmBend();
        bend.applyBend(0.35F, 0.8F);
        float xScale = 1.15F;
        float yScale = 1.035F;
        float zScale = 1.15F;
        float xOffset = -0.998F;
        float yOffset = -0.1F;
        MeshTransformer transformer = SkinLayersBendCompat.createTransformer(
                bend,
                xScale,
                yScale,
                zScale,
                xOffset,
                yOffset,
                0.0F
        );
        Vector4f[] vertices = {
                new Vector4f(-0.125F, 0.625F, 0.125F, 1.0F),
                new Vector4f(0.125F, 0.625F, 0.125F, 1.0F),
                new Vector4f(0.125F, 0.375F, 0.125F, 1.0F),
                new Vector4f(-0.125F, 0.375F, 0.125F, 1.0F)
        };
        RememberingPos expected = new RememberingPos(
                (vertices[0].x() * 16.0F + xOffset) * xScale,
                (vertices[0].y() * 16.0F + yOffset) * yScale,
                vertices[0].z() * 16.0F * zScale
        );
        bend.applyBend(bend.getBendAxis(), bend.getBend(), consumer -> consumer.accept(expected));

        transformer.transform(new Vector3f(0.0F, 0.0F, 1.0F), vertices);

        assertEquals(expected.getPos().x() / xScale - xOffset, vertices[0].x() * 16.0F, 0.00001F);
        assertEquals(expected.getPos().y() / yScale - yOffset, vertices[0].y() * 16.0F, 0.00001F);
        assertEquals(expected.getPos().z() / zScale, vertices[0].z() * 16.0F, 0.00001F);
    }

    @Test
    void appliesSourceBendDirectionToFastRenderCube() {
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

        Object activeMutator = cube.getActiveMutator().getB();
        assertTrue(activeMutator instanceof BendableCuboid);
        BendableCuboid cubeBend = (BendableCuboid) activeMutator;
        assertEquals(sourceBend.getBendAxis(), cubeBend.getBendAxis());
        assertEquals(sourceBend.getBend(), cubeBend.getBend());

        RememberingPos expected = new RememberingPos(0.0F, 8.0F, 2.0F);
        RememberingPos actual = new RememberingPos(0.0F, 8.0F, 2.0F);
        sourceBend.applyBend(sourceBend.getBendAxis(), sourceBend.getBend(), consumer -> consumer.accept(expected));
        cubeBend.applyBend(cubeBend.getBendAxis(), cubeBend.getBend(), consumer -> consumer.accept(actual));
        assertEquals(expected.getPos().x(), actual.getPos().x(), 0.00001F);
        assertEquals(expected.getPos().y(), actual.getPos().y(), 0.00001F);
        assertEquals(expected.getPos().z(), actual.getPos().z(), 0.00001F);
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

    private static BendableCuboid createArmBend() {
        ICuboidBuilder.Data armData = new ICuboidBuilder.Data(
                0, 0,
                -3.0F, -2.0F, -2.0F,
                4.0F, 12.0F, 4.0F,
                0.0F, 0.0F, 0.0F,
                false, 64.0F, 64.0F
        );
        return new BendableCuboid.Builder().setDirection(Direction.UP).build(armData);
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
