package fori.graphics;

public abstract class RenderCommand {
    public ShaderProgram shaderProgram;

    public Buffer stagingVertexBuffer;
    public Buffer stagingIndexBuffer;
    public Buffer vertexBuffer;
    public Buffer indexBuffer;
    public Buffer[] transformsBuffer;
    public Buffer[] cameraBuffer;
    public int indexCount;

    public RenderCommand(int framesInFlight){

    }

    public abstract Buffer getDefaultVertexBuffer();

    public abstract Buffer getDefaultIndexBuffer();

    public abstract void update();
}
