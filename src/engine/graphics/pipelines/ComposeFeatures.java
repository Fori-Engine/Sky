package engine.graphics.pipelines;

import engine.graphics.Buffer;

public class ComposeFeatures extends Features {
    private Buffer vertexBuffer;
    private Buffer indexBuffer;

    protected ComposeFeatures(boolean mandatory) {
        super(mandatory);
    }

    public Buffer getVertexBuffer() {
        return vertexBuffer;
    }

    public void setVertexBuffer(Buffer vertexBuffer) {
        this.vertexBuffer = vertexBuffer;
    }

    public Buffer getIndexBuffer() {
        return indexBuffer;
    }

    public void setIndexBuffer(Buffer indexBuffer) {
        this.indexBuffer = indexBuffer;
    }
}
