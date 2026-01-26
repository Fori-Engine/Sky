package engine.graphics.pipelines;

import engine.graphics.Buffer;

public class ScreenSpaceFeatures extends Features {
    private Buffer vertexBuffer;
    private Buffer indexBuffer;
    private int indexCount;

    protected ScreenSpaceFeatures(boolean mandatory) {
        super(mandatory);
    }

    public Buffer getVertexBuffer() {
        return vertexBuffer;
    }

    protected void setVertexBuffer(Buffer vertexBuffer) {
        this.vertexBuffer = vertexBuffer;
    }

    public Buffer getIndexBuffer() {
        return indexBuffer;
    }

    protected void setIndexBuffer(Buffer indexBuffer) {
        this.indexBuffer = indexBuffer;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public void setIndexCount(int indexCount) {
        this.indexCount = indexCount;
    }
}
