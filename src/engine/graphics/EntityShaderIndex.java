package engine.graphics;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class EntityShaderIndex {
    private int entityIndex;

    public EntityShaderIndex(int entityIndex) {
        this.entityIndex = entityIndex;
    }


    public void upload(MeshData meshData, ShaderProgram shaderProgram, ByteBuffer vertexBufferData, ByteBuffer indexBufferData, int vertexOffset) {

        List<Float> positions = meshData.getData().getOrDefault("Positions", new ArrayList<>());
        List<Float> textureUVs = meshData.getData().getOrDefault("TextureUVs", new ArrayList<>());
        List<Float> colors = meshData.getData().getOrDefault("Colors", new ArrayList<>());

        for (int vertexIndex = 0; vertexIndex < meshData.getVertexCount(); vertexIndex++) {

            for(VertexAttribute vertexAttribute : shaderProgram.getVertexAttributes()) {
                switch (vertexAttribute.getName()) {
                    case "vertex.pos" -> {
                        float x = positions.get(3 * vertexIndex + 0);
                        float y = positions.get(3 * vertexIndex + 1);
                        float z = positions.get(3 * vertexIndex + 2);

                        vertexBufferData.putFloat(x);
                        vertexBufferData.putFloat(y);
                        vertexBufferData.putFloat(z);
                    }
                    case "vertex.entityIndex" -> {
                        vertexBufferData.putFloat(entityIndex);
                    }
                    case "vertex.uv" -> {
                        float u = textureUVs.get(2 * vertexIndex);
                        float v = textureUVs.get(2 * vertexIndex + 1);

                        vertexBufferData.putFloat(u);
                        vertexBufferData.putFloat(v);
                    }
                    case "vertex.color" -> {
                        float r = colors.get(4 * vertexIndex + 0);
                        float g = colors.get(4 * vertexIndex + 1);
                        float b = colors.get(4 * vertexIndex + 2);
                        float a = colors.get(4 * vertexIndex + 3);

                        vertexBufferData.putFloat(r);
                        vertexBufferData.putFloat(g);
                        vertexBufferData.putFloat(b);
                        vertexBufferData.putFloat(a);
                    }
                    default -> {
                        for(int i = 0; i < vertexAttribute.getSize(); i++)
                            vertexBufferData.putFloat(0);
                    }
                }
            }

        }

        for(int index : meshData.getIndices()) {
            indexBufferData.putInt(vertexOffset + index);
        }
    }


}
