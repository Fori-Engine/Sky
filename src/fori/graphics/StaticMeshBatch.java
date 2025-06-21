package fori.graphics;

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
    protected int maxTransformCount;
    protected boolean finalized;

    public StaticMeshBatch(int maxVertexCount, int maxIndexCount, int maxTransformCount, ShaderProgram shaderProgram) {
        this.maxIndexCount = maxIndexCount;
        this.maxVertexCount = maxVertexCount;
        this.maxTransformCount = maxTransformCount;
        this.shaderProgram = shaderProgram;
    }

    public abstract Buffer getDefaultVertexBuffer();
    public abstract Buffer getDefaultIndexBuffer();

    public void uploadsFinished() {
        finalized = true;
    }

    public boolean isFinalized() {
        return finalized;
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

    public abstract void updateMeshBatch(int vertexCount, int indexCount);

}
