package fori.graphics;

public abstract class DynamicMesh {
    protected Buffer[] transformsBuffers;
    protected Buffer[] sceneDescBuffers;
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



    public abstract void submit(Mesh mesh, MeshUploader meshUploader);

    public Buffer getVertexBuffer() {
        return vertexBuffer;
    }
    public Buffer getIndexBuffer() {
        return indexBuffer;
    }


    public Buffer[] getTransformsBuffers() {
        return transformsBuffers;
    }

    public Buffer[] getSceneDescBuffers() {
        return sceneDescBuffers;
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
