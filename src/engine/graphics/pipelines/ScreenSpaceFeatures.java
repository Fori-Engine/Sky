package engine.graphics.pipelines;

import engine.graphics.Buffer;
import engine.graphics.ShaderProgram;

public class ScreenSpaceFeatures extends Features {
    private Buffer[] vertexBuffers;
    private Buffer[] indexBuffers;
    private int indexCount;
    private ShaderProgram shaderProgram;
    private int maxQuads = 1000;

    protected ScreenSpaceFeatures(boolean mandatory) {
        super(mandatory);
    }

    public Buffer[] getVertexBuffers() {
        return vertexBuffers;
    }

    public ShaderProgram getShaderProgram() {
        return shaderProgram;
    }

    protected void setShaderProgram(ShaderProgram shaderProgram) {
        this.shaderProgram = shaderProgram;
    }

    protected void setVertexBuffers(Buffer[] vertexBuffers) {
        this.vertexBuffers = vertexBuffers;
    }

    public Buffer[] getIndexBuffers() {
        return indexBuffers;
    }

    protected void setIndexBuffers(Buffer[] indexBuffers) {
        this.indexBuffers = indexBuffers;
    }

    public int getMaxQuads() {
        return maxQuads;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public void setIndexCount(int indexCount) {
        this.indexCount = indexCount;
    }
}
