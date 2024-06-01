package lake.graphics;

public class RenderBatch {
    public ShaderProgram shaderProgram;
    public int start;
    public int quads;
    public int indices;
    public FastTextureLookup textureLookup = new FastTextureLookup(32);
    public int nextTextureIndex = 0;

    public RenderBatch(ShaderProgram shaderProgram, int start) {
        this.shaderProgram = shaderProgram;
        this.start = start;
    }
}
