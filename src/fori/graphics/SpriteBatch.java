package fori.graphics;

public abstract class SpriteBatch {
    protected Camera camera;
    protected Buffer[] cameraBuffers;
    protected ShaderProgram shaderProgram;
    protected int maxVertexCount;
    protected int maxIndexCount;
    protected int vertexCount;
    protected int indexCount;
    protected Buffer vertexBuffer;
    protected Buffer indexBuffer;

    public SpriteBatch(Ref parent, int maxVertexCount, int maxIndexCount, ShaderProgram shaderProgram, Camera camera) {
        this.maxVertexCount = maxVertexCount;
        this.maxIndexCount = maxIndexCount;
        this.shaderProgram = shaderProgram;
        this.camera = camera;
    }

    public ShaderProgram getShaderProgram() {
        return shaderProgram;
    }

    public Camera getCamera() {
        return camera;
    }

    public int getMaxVertexCount() {
        return maxVertexCount;
    }

    public int getMaxIndexCount() {
        return maxIndexCount;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public Buffer getVertexBuffer() {
        return vertexBuffer;
    }

    public Buffer getIndexBuffer() {
        return indexBuffer;
    }

    public Buffer[] getCameraBuffers() {
        return cameraBuffers;
    }

    public abstract void start();
    public abstract void end();
    public abstract void drawRect(float x, float y, float w, float h, Color color);
}
