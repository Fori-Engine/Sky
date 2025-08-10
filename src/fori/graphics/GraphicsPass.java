package fori.graphics;

public abstract class GraphicsPass extends Pass {
    protected RenderTarget renderTarget;

    public GraphicsPass(Disposable parent, String name, int framesInFlight) {
        super(parent, name, framesInFlight);
    }

    public RenderTarget getRenderTarget() {
        return renderTarget;
    }

    public void startRendering(RenderTarget renderTarget, boolean clear) {
        this.renderTarget = renderTarget;
    }
    public abstract void endRendering();

    public abstract void setDrawBuffers(Buffer vertexBuffer, Buffer indexBuffer);
    public abstract void setShaderProgram(ShaderProgram shaderProgram);
    public abstract void drawIndexed(int indexCount);
}
