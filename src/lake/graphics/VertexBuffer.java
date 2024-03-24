package lake.graphics;

public abstract class VertexBuffer {
    protected int maxQuads;
    protected int vertexDataSize;

    public VertexBuffer(int maxQuads, int vertexDataSize) {
        this.maxQuads = maxQuads;
        this.vertexDataSize = vertexDataSize;
    }

    public abstract int getNumOfVertices();
    public abstract void build();
    public int maxQuads() {
        return maxQuads;
    }
    public int getVertexDataSize() {
        return vertexDataSize;
    }
    public abstract void dispose();
}
