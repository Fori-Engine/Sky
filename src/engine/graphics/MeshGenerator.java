package engine.graphics;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeshGenerator {

    public static MeshData newPlane(float width, float depth) {


        float hw = width / 2.0f;
        float hd = depth / 2.0f;

        List<Float> verticesList = new ArrayList<>();
        List<Float> colorsList = new ArrayList<>();
        List<Float> normalsList = new ArrayList<>();
        List<Float> tangentsList = new ArrayList<>();
        List<Float> textureUVsList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();


        Vector3f[] vertices = {
                new Vector3f(-hw,  0,  hd),
                new Vector3f( hw,  0,  hd),
                new Vector3f( hw,  0, -hd),
                new Vector3f(-hw,  0, -hd)
        };

        Vector2f[] textureUVs = {
                new Vector2f(0.0f, 1.0f),
                new Vector2f(1.0f, 1.0f),
                new Vector2f(1.0f, 0.0f),
                new Vector2f(0.0f, 0.0f),

        };

        float[][] colors = {
                {1f, 1f, 1f, 1f}, // Rich red
        };


        float[] faceColors = colors[0];

        Vector3f e0 = new Vector3f(vertices[1]).sub(vertices[0]);
        Vector3f e1 = new Vector3f(vertices[2]).sub(vertices[0]);

        Vector3f faceNormal = new Vector3f(e0).cross(e1).normalize();
        Vector3f faceTangent = new Vector3f(e0).normalize();

        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            Vector3f vertex = vertices[vertexIndex];
            Vector2f uv =  textureUVs[vertexIndex];


            // Position
            verticesList.add(vertex.x);
            verticesList.add(vertex.y);
            verticesList.add(vertex.z);

            // Color
            colorsList.add(faceColors[0]);
            colorsList.add(faceColors[1]);
            colorsList.add(faceColors[2]);
            colorsList.add(faceColors[3]);

            //Normals
            normalsList.add(faceNormal.x);
            normalsList.add(faceNormal.y);
            normalsList.add(faceNormal.z);

            //Tangents
            tangentsList.add(faceTangent.x);
            tangentsList.add(faceTangent.y);
            tangentsList.add(faceTangent.z);

            //UVs
            textureUVsList.add(uv.x);
            textureUVsList.add(uv.y);

        }

        // Indices for the face (2 triangles)
        indicesList.add(0);
        indicesList.add(1);
        indicesList.add(2);
        indicesList.add(2);
        indicesList.add(3);
        indicesList.add(0);

        Map<String, List<Float>> vertexData = new HashMap<>();
        vertexData.put("Positions", verticesList);
        vertexData.put("Colors", colorsList);
        vertexData.put("Normals", normalsList);
        vertexData.put("TextureUVs", textureUVsList);
        vertexData.put("Tangents", tangentsList);

        return new MeshData(vertexData, indicesList, 4);
    }

    public static MeshData newBox(float width, float height, float depth) {


        float hw = width / 2.0f;
        float hh = height / 2.0f;
        float hd = depth / 2.0f;

        List<Float> verticesList = new ArrayList<>();
        List<Float> colorsList = new ArrayList<>();
        List<Float> normalsList = new ArrayList<>();
        List<Float> tangentsList = new ArrayList<>();
        List<Float> textureUVsList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();


        Vector3f[][] vertices = {
                // Front
                {
                        new Vector3f(-hw, -hh,  hd),
                        new Vector3f( hw, -hh,  hd),
                        new Vector3f( hw,  hh,  hd),
                        new Vector3f(-hw,  hh,  hd)
                },
                // Back
                {
                        new Vector3f( hw, -hh, -hd),
                        new Vector3f(-hw, -hh, -hd),
                        new Vector3f(-hw,  hh, -hd),
                        new Vector3f( hw,  hh, -hd)
                },
                // Left
                {
                        new Vector3f(-hw, -hh, -hd),
                        new Vector3f(-hw, -hh,  hd),
                        new Vector3f(-hw,  hh,  hd),
                        new Vector3f(-hw,  hh, -hd)
                },
                // Right
                {
                        new Vector3f( hw, -hh,  hd),
                        new Vector3f( hw, -hh, -hd),
                        new Vector3f( hw,  hh, -hd),
                        new Vector3f( hw,  hh,  hd)
                },
                // Top
                {
                        new Vector3f(-hw,  hh,  hd),
                        new Vector3f( hw,  hh,  hd),
                        new Vector3f( hw,  hh, -hd),
                        new Vector3f(-hw,  hh, -hd)
                },
                // Bottom
                {
                        new Vector3f(-hw, -hh, -hd),
                        new Vector3f( hw, -hh, -hd),
                        new Vector3f( hw, -hh,  hd),
                        new Vector3f(-hw, -hh,  hd)
                }
        };

        float[][] colors = {
                {1, 1, 1, 1},
                {1, 1, 1, 1},
                {1, 1, 1, 1},
                {1, 1, 1, 1},
                {1, 1, 1, 1},
                {1, 1, 1, 1}
        };

        Vector2f[] textureUVs = {
                new Vector2f(0.0f, 1.0f),
                new Vector2f(1.0f, 1.0f),
                new Vector2f(1.0f, 0.0f),
                new Vector2f(0.0f, 0.0f),

        };

        int index = 0;
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            Vector3f[] faceVertices = vertices[faceIndex];
            float[] faceColors = colors[faceIndex];

            Vector3f e0 = new Vector3f(faceVertices[1]).sub(faceVertices[0]);
            Vector3f e1 = new Vector3f(faceVertices[2]).sub(faceVertices[0]);

            Vector3f faceNormal = new Vector3f(e0).cross(e1).normalize();
            Vector3f faceTangent = new Vector3f(e0).normalize();


            for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                Vector3f vertex = faceVertices[vertexIndex];
                Vector2f uv =  textureUVs[vertexIndex];

                // Position
                verticesList.add(vertex.x);
                verticesList.add(vertex.y);
                verticesList.add(vertex.z);

                // Color
                colorsList.add(faceColors[0]);
                colorsList.add(faceColors[1]);
                colorsList.add(faceColors[2]);
                colorsList.add(faceColors[3]);

                //Normals
                normalsList.add(faceNormal.x);
                normalsList.add(faceNormal.y);
                normalsList.add(faceNormal.z);

                //Tangents
                tangentsList.add(faceTangent.x);
                tangentsList.add(faceTangent.y);
                tangentsList.add(faceTangent.z);

                textureUVsList.add(uv.x);
                textureUVsList.add(uv.y);

            }

            // Indices for the face (2 triangles)
            indicesList.add(index);
            indicesList.add(index + 1);
            indicesList.add(index + 2);
            indicesList.add(index);
            indicesList.add(index + 2);
            indicesList.add(index + 3);

            index += 4;
        }

        Map<String, List<Float>> vertexData = new HashMap<>();
        vertexData.put("Positions", verticesList);
        vertexData.put("Colors", colorsList);
        vertexData.put("Normals", normalsList);
        vertexData.put("TextureUVs", textureUVsList);
        vertexData.put("Tangents", tangentsList);

        return new MeshData(vertexData, indicesList, 24);
    }

    public static MeshData newCylinder(float radius, float height, int segments) {

        float halfHeight = height / 2.0f;

        List<Float> verticesList = new ArrayList<>();
        List<Float> colorsList = new ArrayList<>();
        List<Float> normalsList = new ArrayList<>();
        List<Float> tangentsList = new ArrayList<>();
        List<Float> textureUVsList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();

        // Generate side vertices
        for (int i = 0; i <= segments; i++) {

            float angle = (float)(2.0 * Math.PI * i / segments);
            float cos = (float)Math.cos(angle);
            float sin = (float)Math.sin(angle);

            float x = radius * cos;
            float z = radius * sin;

            Vector3f normal = new Vector3f(cos, 0, sin).normalize();
            Vector3f tangent = new Vector3f(-sin, 0, cos).normalize();

            float u = (float)i / segments;

            // Top vertex
            verticesList.add(x);
            verticesList.add(halfHeight);
            verticesList.add(z);

            colorsList.add(1f); colorsList.add(1f); colorsList.add(1f); colorsList.add(1f);

            normalsList.add(normal.x);
            normalsList.add(normal.y);
            normalsList.add(normal.z);

            tangentsList.add(tangent.x);
            tangentsList.add(tangent.y);
            tangentsList.add(tangent.z);

            textureUVsList.add(u);
            textureUVsList.add(1f);

            // Bottom vertex
            verticesList.add(x);
            verticesList.add(-halfHeight);
            verticesList.add(z);

            colorsList.add(1f); colorsList.add(1f); colorsList.add(1f); colorsList.add(1f);

            normalsList.add(normal.x);
            normalsList.add(normal.y);
            normalsList.add(normal.z);

            tangentsList.add(tangent.x);
            tangentsList.add(tangent.y);
            tangentsList.add(tangent.z);

            textureUVsList.add(u);
            textureUVsList.add(0f);
        }

        // Side indices
        for (int i = 0; i < segments; i++) {

            int k1 = i * 2;
            int k2 = k1 + 1;
            int k3 = k1 + 2;
            int k4 = k1 + 3;

            indicesList.add(k1);
            indicesList.add(k3);
            indicesList.add(k2);

            indicesList.add(k3);
            indicesList.add(k4);
            indicesList.add(k2);
        }

        int topCenterIndex = verticesList.size() / 3;

        // Top center
        verticesList.add(0f);
        verticesList.add(halfHeight);
        verticesList.add(0f);

        colorsList.add(1f); colorsList.add(1f); colorsList.add(1f); colorsList.add(1f);

        normalsList.add(0f); normalsList.add(1f); normalsList.add(0f);

        tangentsList.add(1f); tangentsList.add(0f); tangentsList.add(0f);

        textureUVsList.add(0.5f); textureUVsList.add(0.5f);

        // Top cap
        for (int i = 0; i < segments; i++) {

            int k = i * 2;
            int next = ((i + 1) % segments) * 2;

            indicesList.add(topCenterIndex);
            indicesList.add(next);
            indicesList.add(k);
        }

        int bottomCenterIndex = verticesList.size() / 3;

        // Bottom center
        verticesList.add(0f);
        verticesList.add(-halfHeight);
        verticesList.add(0f);

        colorsList.add(1f); colorsList.add(1f); colorsList.add(1f); colorsList.add(1f);

        normalsList.add(0f); normalsList.add(-1f); normalsList.add(0f);

        tangentsList.add(1f); tangentsList.add(0f); tangentsList.add(0f);

        textureUVsList.add(0.5f); textureUVsList.add(0.5f);

        // Bottom cap
        for (int i = 0; i < segments; i++) {

            int k = i * 2 + 1;
            int next = ((i + 1) % segments) * 2 + 1;

            indicesList.add(bottomCenterIndex);
            indicesList.add(k);
            indicesList.add(next);
        }

        Map<String, List<Float>> vertexData = new HashMap<>();
        vertexData.put("Positions", verticesList);
        vertexData.put("Colors", colorsList);
        vertexData.put("Normals", normalsList);
        vertexData.put("TextureUVs", textureUVsList);
        vertexData.put("Tangents", tangentsList);

        int vertexCount = verticesList.size() / 3;

        return new MeshData(vertexData, indicesList, vertexCount);
    }

    public static MeshData newIcoSphere(float radius, int subdivisions) {

        List<Vector3f> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        // Golden ratio
        float r = (1.0f + (float)Math.sqrt(5.0)) / 2.0f;

        // Create 12 initial vertices of an icosahedron
        vertices.add(new Vector3f(-1,  r,  0));
        vertices.add(new Vector3f( 1,  r,  0));
        vertices.add(new Vector3f(-1, -r,  0));
        vertices.add(new Vector3f( 1, -r,  0));

        vertices.add(new Vector3f( 0, -1,  r));
        vertices.add(new Vector3f( 0,  1,  r));
        vertices.add(new Vector3f( 0, -1, -r));
        vertices.add(new Vector3f( 0,  1, -r));

        vertices.add(new Vector3f( r,  0, -1));
        vertices.add(new Vector3f( r,  0,  1));
        vertices.add(new Vector3f(-r,  0, -1));
        vertices.add(new Vector3f(-r,  0,  1));

        // Normalize to radius
        for (int i = 0; i < vertices.size(); i++)
            vertices.set(i, new Vector3f(vertices.get(i)).normalize().mul(radius));

        // 20 faces of the icosahedron
        int[][] faces = {
                {0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11},
                {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8},
                {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9},
                {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}
        };

        List<int[]> facesList = new ArrayList<>();
        for (int[] f : faces)
            facesList.add(f);

        Map<Long, Integer> middlePointCache = new HashMap<>();

        // Subdivide
        for (int i = 0; i < subdivisions; i++) {
            List<int[]> faces2 = new ArrayList<>();
            for (int[] tri : facesList) {

                int a = tri[0];
                int b = tri[1];
                int c = tri[2];

                int ab = getMiddlePoint(a, b, vertices, middlePointCache, radius);
                int bc = getMiddlePoint(b, c, vertices, middlePointCache, radius);
                int ca = getMiddlePoint(c, a, vertices, middlePointCache, radius);

                faces2.add(new int[]{a, ab, ca});
                faces2.add(new int[]{b, bc, ab});
                faces2.add(new int[]{c, ca, bc});
                faces2.add(new int[]{ab, bc, ca});
            }
            facesList = faces2;
        }

        // Convert facesList to indices
        for (int[] f : facesList) {
            indices.add(f[0]);
            indices.add(f[1]);
            indices.add(f[2]);
        }

        // Build MeshData
        List<Float> positions = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> tangents = new ArrayList<>();
        List<Float> colors = new ArrayList<>();
        List<Float> uvs = new ArrayList<>();

        for (Vector3f v : vertices) {

            positions.add(v.x);
            positions.add(v.y);
            positions.add(v.z);

            Vector3f n = new Vector3f(v).normalize();
            normals.add(n.x); normals.add(n.y); normals.add(n.z);

            Vector3f t = new Vector3f(-v.z, 0, v.x).normalize(); // simple tangent
            tangents.add(t.x); tangents.add(t.y); tangents.add(t.z);

            colors.add(1f); colors.add(1f); colors.add(1f); colors.add(1f);

            // spherical UV mapping
            float u = 0.5f + (float)(Math.atan2(v.z, v.x) / (2 * Math.PI));
            float vTex = 0.5f - (float)(Math.asin(v.y / radius) / Math.PI);
            uvs.add(u);
            uvs.add(vTex);
        }

        Map<String, List<Float>> vertexData = new HashMap<>();
        vertexData.put("Positions", positions);
        vertexData.put("Colors", colors);
        vertexData.put("Normals", normals);
        vertexData.put("Tangents", tangents);
        vertexData.put("TextureUVs", uvs);

        return new MeshData(vertexData, indices, vertices.size());
    }

    // Helper to find middle point to avoid duplicates
    private static int getMiddlePoint(int p1, int p2, List<Vector3f> vertices, Map<Long, Integer> cache, float radius) {
        long smaller = Math.min(p1, p2);
        long greater = Math.max(p1, p2);
        long key = (smaller << 32) + greater;

        if (cache.containsKey(key)) return cache.get(key);

        Vector3f point1 = vertices.get(p1);
        Vector3f point2 = vertices.get(p2);
        Vector3f middle = new Vector3f(
                (point1.x + point2.x)/2.0f,
                (point1.y + point2.y)/2.0f,
                (point1.z + point2.z)/2.0f
        ).normalize().mul(radius);

        vertices.add(middle);
        int index = vertices.size() - 1;
        cache.put(key, index);
        return index;
    }
}
