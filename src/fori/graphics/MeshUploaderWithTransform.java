package fori.graphics;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class MeshUploaderWithTransform implements MeshUploader {

    private int transformIndex;

    public MeshUploaderWithTransform(int transformIndex) {
        this.transformIndex = transformIndex;
    }

    @Override
    public void upload(VertexAttributes.Type vertexAttribute, int vertexIndex, ByteBuffer vertexBufferData, Map<VertexAttributes.Type, List<Float>> vertexData) {
        List<Float> attributeData = vertexData.get(vertexAttribute);


        if(vertexAttribute == VertexAttributes.Type.PositionFloat3){
            vertexBufferData.putFloat(attributeData.get(vertexAttribute.size * vertexIndex));
            vertexBufferData.putFloat(attributeData.get(vertexAttribute.size * vertexIndex + 1));
            vertexBufferData.putFloat(attributeData.get(vertexAttribute.size * vertexIndex + 2));
        }

        else if(vertexAttribute == VertexAttributes.Type.UVFloat2){
            vertexBufferData.putFloat(attributeData.get(vertexAttribute.size * vertexIndex));
            vertexBufferData.putFloat(attributeData.get(vertexAttribute.size * vertexIndex + 1));
        }
        else if(vertexAttribute == VertexAttributes.Type.ColorFloat4){
            vertexBufferData.putFloat(attributeData.get(vertexAttribute.size * vertexIndex));
            vertexBufferData.putFloat(attributeData.get(vertexAttribute.size * vertexIndex + 1));
            vertexBufferData.putFloat(attributeData.get(vertexAttribute.size * vertexIndex + 2));
            vertexBufferData.putFloat(attributeData.get(vertexAttribute.size * vertexIndex + 3));
        }

        else if(vertexAttribute == VertexAttributes.Type.TransformIndexFloat1) vertexBufferData.putFloat(transformIndex);

    }
}
