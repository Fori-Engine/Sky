package fori.graphics;

public class RenderQueueFlags {
    public enum DepthOp {
        LessThan,
        GreaterThan,
        LessOrEqualTo,
        GreaterOrEqualTo,
        Always
    }

    public boolean depthTest;
    public DepthOp depthOp;
    public int maxVertices;
    public int maxIndices;
    public ShaderProgram shaderProgram;

}
