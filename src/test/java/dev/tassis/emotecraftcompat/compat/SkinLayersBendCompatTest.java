package dev.tassis.emotecraftcompat.compat;

import dev.tr7zw.skinlayers.api.MeshTransformer;
import io.github.kosmx.bendylib.ICuboidBuilder;
import io.github.kosmx.bendylib.impl.BendableCuboid;
import io.github.kosmx.bendylib.impl.RememberingPos;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void preservesTheOriginalNormalRelationshipForBothVertexOrders() {
        assertPreservesWindingRelationship(new Vector4f[] {
                vertex(-2.0F, 12.0F, 2.0F),
                vertex(2.0F, 12.0F, 2.0F),
                vertex(2.0F, 8.0F, 2.0F),
                vertex(-2.0F, 8.0F, 2.0F)
        });
        assertPreservesWindingRelationship(new Vector4f[] {
                vertex(-2.0F, 12.0F, 2.0F),
                vertex(-2.0F, 8.0F, 2.0F),
                vertex(2.0F, 8.0F, 2.0F),
                vertex(2.0F, 12.0F, 2.0F)
        });
    }

    @Test
    void allowsNormalToRotatePastTheOriginalNormalHemisphere() {
        assertNormalRotatesPastOriginalHemisphere(new Vector4f[] {
                vertex(-2.0F, 13.0F, 2.0F),
                vertex(2.0F, 13.0F, 2.0F),
                vertex(2.0F, 12.0F, 2.0F),
                vertex(-2.0F, 12.0F, 2.0F)
        });
        assertNormalRotatesPastOriginalHemisphere(new Vector4f[] {
                vertex(-2.0F, 13.0F, 2.0F),
                vertex(-2.0F, 12.0F, 2.0F),
                vertex(2.0F, 12.0F, 2.0F),
                vertex(2.0F, 13.0F, 2.0F)
        });
    }

    @Test
    void usesTheFallbackTriangleWhenTheFirstTriangleIsDegenerate() {
        MeshTransformer transformer = createLegTransformer(0.0F);
        Vector3f normal = new Vector3f(0.0F, 0.0F, 1.0F);
        Vector4f[] vertices = {
                vertex(0.0F, 0.0F, 0.0F),
                vertex(1.0F, 0.0F, 0.0F),
                vertex(2.0F, 0.0F, 0.0F),
                vertex(0.0F, 1.0F, 0.0F)
        };

        transformer.transform(normal, vertices);

        assertEquals(0.0F, normal.x(), 0.00001F);
        assertEquals(0.0F, normal.y(), 0.00001F);
        assertEquals(1.0F, normal.z(), 0.00001F);
    }

    @Test
    void keepsNormalWhenTheFaceCannotProvideAReliableOrientation() {
        MeshTransformer transformer = createLegTransformer(0.0F);
        Vector4f[] face = {
                vertex(-2.0F, 12.0F, 2.0F),
                vertex(2.0F, 12.0F, 2.0F),
                vertex(2.0F, 8.0F, 2.0F),
                vertex(-2.0F, 8.0F, 2.0F)
        };
        Vector3f zeroNormal = new Vector3f();
        Vector3f orthogonalNormal = new Vector3f(1.0F, 0.0F, 0.0F);
        Vector3f nonFiniteNormal = new Vector3f(Float.NaN, 0.0F, 1.0F);

        transformer.transform(zeroNormal, copyVertices(face));
        transformer.transform(orthogonalNormal, copyVertices(face));
        transformer.transform(nonFiniteNormal, copyVertices(face));

        assertEquals(new Vector3f(), zeroNormal);
        assertEquals(new Vector3f(1.0F, 0.0F, 0.0F), orthogonalNormal);
        assertTrue(Float.isNaN(nonFiniteNormal.x()));
        assertEquals(0.0F, nonFiniteNormal.y(), 0.0F);
        assertEquals(1.0F, nonFiniteNormal.z(), 0.0F);
    }

    @Test
    void keepsNormalWhenTheWholeFaceIsDegenerate() {
        MeshTransformer transformer = createLegTransformer(0.0F);
        Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector4f repeated = vertex(0.0F, 0.0F, 0.0F);
        Vector4f[] vertices = {
                new Vector4f(repeated),
                new Vector4f(repeated),
                new Vector4f(repeated),
                new Vector4f(repeated)
        };

        transformer.transform(normal, vertices);

        assertEquals(new Vector3f(0.0F, 1.0F, 0.0F), normal);
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

    private static MeshTransformer createLegTransformer(float bendAngle) {
        ICuboidBuilder.Data legData = new ICuboidBuilder.Data(
                0, 0,
                -2.0F, 0.0F, -2.0F,
                4.0F, 12.0F, 4.0F,
                0.0F, 0.0F, 0.0F,
                false, 64.0F, 64.0F
        );
        BendableCuboid bend = new BendableCuboid.Builder().setDirection(Direction.UP).build(legData);
        bend.applyBend(0.0F, bendAngle);
        return SkinLayersBendCompat.createTransformer(
                bend,
                1.0F,
                1.0F,
                1.0F,
                0.0F,
                0.0F,
                0.0F
        );
    }

    private static void assertNormalRotatesPastOriginalHemisphere(Vector4f[] vertices) {
        MeshTransformer transformer = createLegTransformer((float) Math.toRadians(120.0D));
        Vector3f normal = new Vector3f(0.0F, 0.0F, 1.0F);
        Vector3f originalNormal = new Vector3f(normal);
        float originalRelationship = windingRelationship(normal, vertices);

        transformer.transform(normal, vertices);

        Vector3f expectedNormal = geometricNormal(vertices).normalize().mul(originalRelationship);
        assertTrue(
                expectedNormal.dot(originalNormal) < 0.0F,
                "test face must actually rotate its geometric normal beyond 90 degrees"
        );
        assertTrue(normal.dot(originalNormal) < 0.0F, "normal should be allowed to rotate beyond 90 degrees");
        assertTrue(normal.dot(expectedNormal) > 0.99999F, "normal should match the oriented geometric normal");
        assertEquals(
                originalRelationship,
                windingRelationship(normal, vertices),
                0.0F,
                "bend must preserve the original winding-to-normal relationship"
        );
    }

    private static void assertPreservesWindingRelationship(Vector4f[] vertices) {
        MeshTransformer transformer = createLegTransformer(0.8F);
        Vector3f normal = new Vector3f(0.0F, 0.0F, 1.0F);
        float originalRelationship = windingRelationship(normal, vertices);

        transformer.transform(normal, vertices);

        assertEquals(
                originalRelationship,
                windingRelationship(normal, vertices),
                0.0F,
                "bend must preserve whether the supplied normal agrees with the vertex winding"
        );
    }

    private static float windingRelationship(Vector3f normal, Vector4f[] vertices) {
        return Math.signum(normal.dot(geometricNormal(vertices)));
    }

    private static Vector3f geometricNormal(Vector4f[] vertices) {
        Vector3f normal = edge(vertices[1], vertices[0]).cross(edge(vertices[2], vertices[0]));
        if (normal.lengthSquared() < 0.000001F) {
            normal = edge(vertices[2], vertices[0]).cross(edge(vertices[3], vertices[0]));
        }
        return normal;
    }

    private static Vector3f edge(Vector4f left, Vector4f right) {
        return new Vector3f(left.x() - right.x(), left.y() - right.y(), left.z() - right.z());
    }

    private static Vector4f vertex(float x, float y, float z) {
        return new Vector4f(x / 16.0F, y / 16.0F, z / 16.0F, 1.0F);
    }

    private static Vector4f[] copyVertices(Vector4f[] vertices) {
        Vector4f[] copy = new Vector4f[vertices.length];
        for (int index = 0; index < vertices.length; index++) {
            copy[index] = new Vector4f(vertices[index]);
        }
        return copy;
    }
}
