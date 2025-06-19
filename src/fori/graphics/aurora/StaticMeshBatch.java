package fori.graphics.aurora;

import fori.graphics.Buffer;
import fori.graphics.ShaderProgram;

public abstract class StaticMeshBatch {
    public int textureCount;
    public Buffer[] transformsBuffers;
    public Buffer[] cameraBuffers;
    public Buffer vertexBuffer;
    public Buffer indexBuffer;
    public ShaderProgram shaderProgram;
    public int vertexCount;
    public int indexCount;

    public int maxVertexCount = 100000;
    public int maxIndexCount = 100000;

    public abstract Buffer getDefaultVertexBuffer();
    public abstract Buffer getDefaultIndexBuffer();

    public Buffer getVertexBuffer() {
        return vertexBuffer;
    }

    public Buffer getIndexBuffer() {
        return indexBuffer;
    }

    public abstract void updateMeshBatch(int vertexCount, int indexCount);

}
