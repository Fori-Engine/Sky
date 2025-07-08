package fori.graphics;

public abstract class GraphicsCommandList extends CommandList {
    public GraphicsCommandList(Disposable parent, int framesInFlight) {
        super(parent, framesInFlight);
    }

    public abstract void setDrawBuffers(Buffer vertexBuffer, Buffer indexBuffer);
    public abstract void setShaderProgram(ShaderProgram shaderProgram);
    public abstract void drawIndexed(int indexCount);
}
