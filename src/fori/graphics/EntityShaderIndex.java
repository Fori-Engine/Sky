package fori.graphics;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT;

public class EntityShaderIndex {
    private int entityIndex;

    public EntityShaderIndex(int entityIndex) {
        this.entityIndex = entityIndex;
    }


    public void upload(MeshData meshData, ShaderProgram shaderProgram, ByteBuffer vertexBufferData, ByteBuffer indexBufferData, int vertexOffset) {

        List<Float> positions = meshData.getData().get("Positions");
        List<Float> textureUVs = meshData.getData().get("TextureUVs");

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
                }
            }

        }

        for(int index : meshData.getIndices()) {
            indexBufferData.putInt(vertexOffset + index);
        }
    }


}
