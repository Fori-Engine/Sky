package fori.graphics;

public class RenderBatch {
    public ShaderProgram shaderProgram;
    public int start;
    public int quads;
    public int indices;
    public FastTextureLookup textureLookup;
    public int nextTextureIndex = 0;

    public RenderBatch(ShaderProgram shaderProgram, int start, int maxBindlessSamplers) {
        this.shaderProgram = shaderProgram;
        this.start = start;
        this.textureLookup = new FastTextureLookup(maxBindlessSamplers);
    }
}
