package lake.graphics;

public class RenderBatch {
    public ShaderProgram shaderProgram;
    public int start;
    public int quads;
    public int indices;

    public RenderBatch(ShaderProgram shaderProgram, int start) {
        this.shaderProgram = shaderProgram;
        this.start = start;
    }
}
