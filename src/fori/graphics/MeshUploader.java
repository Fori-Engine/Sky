package fori.graphics;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface MeshUploader {
    void upload(VertexAttributes.Type vertexAttribute, int vertexIndex, ByteBuffer vertexBufferData, Map<VertexAttributes.Type, List<Float>> vertexData);
}
