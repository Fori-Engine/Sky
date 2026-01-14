package engine.graphics;

import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeshGenerator {
    public static MeshData newBox(float width, float height, float depth) {


        float hw = width / 2.0f;
        float hh = height / 2.0f;
        float hd = depth / 2.0f;

        List<Float> verticesList = new ArrayList<>();
        List<Float> colorsList = new ArrayList<>();
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
                {0.5f, 0.5f, 0.5f, 1f},
                {0.6f, 0.6f, 0.6f, 1f},
                {0.7f, 0.7f, 0.7f, 1f},
                {0.8f, 0.8f, 0.8f, 1f},
                {0.9f, 0.9f, 0.9f, 1f},
                {0.5f, 0.5f, 0.5f, 1f}
        };

        int index = 0;
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            Vector3f[] faceVertices = vertices[faceIndex];
            float[] faceColors = colors[faceIndex];



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

        return new MeshData(vertexData, indicesList, 24);
    }
}
