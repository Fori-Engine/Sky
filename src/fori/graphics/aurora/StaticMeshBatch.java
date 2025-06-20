package fori.graphics.aurora;

import fori.graphics.Buffer;
import fori.graphics.ShaderProgram;

public abstract class StaticMeshBatch {
    protected Buffer[] transformsBuffers;
    protected Buffer[] cameraBuffers;
    protected Buffer vertexBuffer;
    protected Buffer indexBuffer;
    public ShaderProgram shaderProgram;
    public int vertexCount;
    public int indexCount;
    protected int maxVertexCount;
    protected int maxIndexCount;

    public StaticMeshBatch(int maxIndexCount, int maxVertexCount, ShaderProgram shaderProgram) {
        this.maxIndexCount = maxIndexCount;
        this.maxVertexCount = maxVertexCount;
        this.shaderProgram = shaderProgram;
    }

    public abstract Buffer getDefaultVertexBuffer();
    public abstract Buffer getDefaultIndexBuffer();

    public Buffer getVertexBuffer() {
        return vertexBuffer;
    }
    public Buffer getIndexBuffer() {
        return indexBuffer;
    }

    public Buffer[] getTransformsBuffers() {
        return transformsBuffers;
    }

    public Buffer[] getCameraBuffers() {
        return cameraBuffers;
    }

    public ShaderProgram getShaderProgram() {
        return shaderProgram;
    }

    public abstract void updateMeshBatch(int vertexCount, int indexCount);

}
