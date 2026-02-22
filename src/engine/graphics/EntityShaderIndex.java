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

        List<Float> positions = meshData.getData().get("Positions");
        List<Float> textureUVs = meshData.getData().get("TextureUVs");
        List<Float> colors = meshData.getData().get("Colors");
        List<Float> normals = meshData.getData().get("Normals");
        List<Float> tangents = meshData.getData().get("Tangents");

        for (int vertexIndex = 0; vertexIndex < meshData.getVertexCount(); vertexIndex++) {


            for(VertexAttribute vertexAttribute : shaderProgram.getVertexAttributes()) {
                switch (vertexAttribute.getName()) {
                    case "vertex.pos_ws" -> {
                        float x = positions.get(3 * vertexIndex + 0);
                        float y = positions.get(3 * vertexIndex + 1);
                        float z = positions.get(3 * vertexIndex + 2);

                        vertexBufferData.putFloat(x);
                        vertexBufferData.putFloat(y);
                        vertexBufferData.putFloat(z);
                    }
                    case "vertex.entity_index" -> {
                        vertexBufferData.putFloat(entityIndex);
                    }
                    case "vertex.uv_ts" -> {
                        float u = textureUVs.get(2 * vertexIndex + 0);
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
                    case "vertex.normal_ws" -> {
                        float nx = normals.get(3 * vertexIndex + 0);
                        float ny = normals.get(3 * vertexIndex + 1);
                        float nz = normals.get(3 * vertexIndex + 2);

                        vertexBufferData.putFloat(nx);
                        vertexBufferData.putFloat(ny);
                        vertexBufferData.putFloat(nz);
                    }
                    case "vertex.tangent_ws" -> {
                        float tx = tangents.get(3 * vertexIndex + 0);
                        float ty = tangents.get(3 * vertexIndex + 1);
                        float tz = tangents.get(3 * vertexIndex + 2);

                        vertexBufferData.putFloat(tx);
                        vertexBufferData.putFloat(ty);
                        vertexBufferData.putFloat(tz);
                    }
                    default -> {
                        for(int i = 0; i < vertexAttribute.getSize(); i++)
                            vertexBufferData.putFloat(0);
                    }
                }
            }

        }

        System.out.println();


        for(int index : meshData.getIndices()) {
            indexBufferData.putInt(vertexOffset + index);
        }
    }


}
