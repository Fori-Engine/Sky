package lake.graphics;

public abstract class VertexBuffer implements Disposable {
    protected int maxQuads;
    protected int vertexSizeBytes;

    public VertexBuffer(int maxQuads, int vertexDataSize) {
        this.maxQuads = maxQuads;
        this.vertexSizeBytes = vertexDataSize;
    }

    public abstract int getNumOfVertices();
    public abstract void build();
    public int maxQuads() {
        return maxQuads;
    }
    public int getVertexSizeBytes() {
        return vertexSizeBytes;
    }

    @Override
    public abstract void dispose();
}
