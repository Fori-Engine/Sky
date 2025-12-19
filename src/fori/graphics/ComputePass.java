package fori.graphics;

public abstract class ComputePass extends Pass {
    protected ShaderProgram shaderProgram;
    public ComputePass(Disposable parent, String name, int framesInFlight) {
        super(parent, name, framesInFlight);
    }
    public abstract void setShaderProgram(ShaderProgram shaderProgram);
    public abstract void dispatch(int groupCountX, int groupCountY, int groupCountZ, int[] shaderMode);
}
