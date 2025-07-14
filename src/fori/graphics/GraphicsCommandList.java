package fori.graphics;

public abstract class GraphicsCommandList extends CommandList {
    protected RenderTarget renderTarget;

    public GraphicsCommandList(Disposable parent, int framesInFlight) {
        super(parent, framesInFlight);
    }

    public RenderTarget getRenderTarget() {
        return renderTarget;
    }

    public void setRenderTarget(RenderTarget renderTarget, boolean clear) {
        this.renderTarget = renderTarget;
    }
    public abstract void flushRenderTarget();

    public abstract void setDrawBuffers(Buffer vertexBuffer, Buffer indexBuffer);
    public abstract void setShaderProgram(ShaderProgram shaderProgram);
    public abstract void drawIndexed(int indexCount);
    public abstract void setPresentable(RenderTarget renderTarget);
}
