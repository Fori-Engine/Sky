package fori.graphics;

public abstract class DynamicMesh {
    protected Buffer[] transformsBuffers;
    protected Buffer[] cameraBuffers;
    protected ShaderProgram shaderProgram;
    protected int maxVertexCount;
    protected int maxIndexCount;
    protected int vertexCount;
    protected int indexCount;
    protected Buffer vertexBuffer;
    protected Buffer indexBuffer;
    protected boolean finalized;

    public DynamicMesh(int maxVertexCount, int maxIndexCount, ShaderProgram shaderProgram) {
        this.maxVertexCount = maxVertexCount;
        this.maxIndexCount = maxIndexCount;
        this.shaderProgram = shaderProgram;
    }

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

    public int getVertexCount() {
        return vertexCount;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public void updateMesh(int vertexCount, int indexCount) {
        finalized = true;
    }

    public boolean isFinalized() {
        return finalized;
    }
}
