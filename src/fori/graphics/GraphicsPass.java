package fori.graphics;

public abstract class GraphicsPass extends Pass {
    protected RenderTarget renderTarget;
    protected ShaderProgram shaderProgram;

    public GraphicsPass(Disposable parent, String name, int framesInFlight) {
        super(parent, name, framesInFlight);
    }

    public RenderTarget getRenderTarget() {
        return renderTarget;
    }

    public abstract void startRendering(RenderTarget renderTarget, int width, int height, boolean clear, Color clearColor);
    public abstract void endRendering();

    public abstract void setDrawBuffers(Buffer vertexBuffer, Buffer indexBuffer);
    public abstract void setShaderProgram(ShaderProgram shaderProgram);
    public abstract void drawIndexed(int indexCount, int[] shaderMode);
}
