package engine.graphics;

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

        List<Float> vertices = new ArrayList<>();
        List<Float> colors = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();


        Vector4f[][] faceVertices = {
                // Front
                {
                        new Vector4f(-hw, -hh,  hd, 1),
                        new Vector4f( hw, -hh,  hd, 1),
                        new Vector4f( hw,  hh,  hd, 1),
                        new Vector4f(-hw,  hh,  hd, 1)
                },
                // Back
                {
                        new Vector4f( hw, -hh, -hd, 1),
                        new Vector4f(-hw, -hh, -hd, 1),
                        new Vector4f(-hw,  hh, -hd, 1),
                        new Vector4f( hw,  hh, -hd, 1)
                },
                // Left
                {
                        new Vector4f(-hw, -hh, -hd, 1),
                        new Vector4f(-hw, -hh,  hd, 1),
                        new Vector4f(-hw,  hh,  hd, 1),
                        new Vector4f(-hw,  hh, -hd, 1)
                },
                // Right
                {
                        new Vector4f( hw, -hh,  hd, 1),
                        new Vector4f( hw, -hh, -hd, 1),
                        new Vector4f( hw,  hh, -hd, 1),
                        new Vector4f( hw,  hh,  hd, 1)
                },
                // Top
                {
                        new Vector4f(-hw,  hh,  hd, 1),
                        new Vector4f( hw,  hh,  hd, 1),
                        new Vector4f( hw,  hh, -hd, 1),
                        new Vector4f(-hw,  hh, -hd, 1)
                },
                // Bottom
                {
                        new Vector4f(-hw, -hh, -hd, 1),
                        new Vector4f( hw, -hh, -hd, 1),
                        new Vector4f( hw, -hh,  hd, 1),
                        new Vector4f(-hw, -hh,  hd, 1)
                }
        };

        float[][] faceColors = {
                {0.5f, 0.5f, 0.5f, 1f},
                {0.6f, 0.6f, 0.6f, 1f},
                {0.7f, 0.7f, 0.7f, 1f},
                {0.8f, 0.8f, 0.8f, 1f},
                {0.9f, 0.9f, 0.9f, 1f},
                {0.5f, 0.5f, 0.5f, 1f}
        };

        int index = 0;
        for (int i = 0; i < 6; i++) {
            Vector4f[] quad = faceVertices[i];
            float[] color = faceColors[i];

            for (int j = 0; j < 4; j++) {
                Vector4f v = quad[j];

                // Position
                vertices.add(v.x);
                vertices.add(v.y);
                vertices.add(v.z);

                // Color
                colors.add(color[0]);
                colors.add(color[1]);
                colors.add(color[2]);
                colors.add(color[3]);
            }

            // Indices for the face (2 triangles)
            indices.add(index);
            indices.add(index + 1);
            indices.add(index + 2);
            indices.add(index);
            indices.add(index + 2);
            indices.add(index + 3);

            index += 4;
        }

        Map<String, List<Float>> vertexData = new HashMap<>();
        vertexData.put("Positions", vertices);
        vertexData.put("Colors", colors);

        return new MeshData(vertexData, indices, 24);
    }
}
