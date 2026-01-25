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

        Vector3f faceNormal = e0.cross(e1).normalize();

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

        return new MeshData(vertexData, indicesList, 4);
    }



    public static MeshData newBox(float width, float height, float depth) {


        float hw = width / 2.0f;
        float hh = height / 2.0f;
        float hd = depth / 2.0f;

        List<Float> verticesList = new ArrayList<>();
        List<Float> colorsList = new ArrayList<>();
        List<Float> normalsList = new ArrayList<>();
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
                {0.95f, 0.22f, 0.30f, 1f}, // Rich red
                {0.18f, 0.85f, 0.35f, 1f}, // Vibrant green
                {0.18f, 0.60f, 0.95f, 1f}, // Vibrant blue
                {1f, 0.82f, 0.18f, 1f}, // Warm yellow
                {0.12f, 0.90f, 0.88f, 1f}, // Soft cyan
                {0.68f, 0.28f, 1f, 1f}  // Deep violet
        };

        int index = 0;
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            Vector3f[] faceVertices = vertices[faceIndex];
            float[] faceColors = colors[faceIndex];

            Vector3f e0 = new Vector3f(faceVertices[1]).sub(faceVertices[0]);
            Vector3f e1 = new Vector3f(faceVertices[2]).sub(faceVertices[0]);

            Vector3f faceNormal = e0.cross(e1).normalize();


            for (int j = 0; j < 4; j++) {
                Vector3f faceVertex = faceVertices[j];

                // Position
                verticesList.add(faceVertex.x);
                verticesList.add(faceVertex.y);
                verticesList.add(faceVertex.z);

                // Color
                colorsList.add(faceColors[0]);
                colorsList.add(faceColors[1]);
                colorsList.add(faceColors[2]);
                colorsList.add(faceColors[3]);

                //Normals
                normalsList.add(faceNormal.x);
                normalsList.add(faceNormal.y);
                normalsList.add(faceNormal.z);

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

        return new MeshData(vertexData, indicesList, 24);
    }
}
