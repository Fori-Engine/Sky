package fori.graphics;

public abstract class ComputeCommandList extends CommandList {
    public ComputeCommandList(Disposable parent, int framesInFlight) {
        super(parent, framesInFlight);
    }

    public abstract void copyTextures(Texture src, Texture dst);
    public abstract void setShaderProgram(ShaderProgram shaderProgram);
    public abstract void dispatch(int groupCountX, int groupCountY, int groupCountZ);
}
