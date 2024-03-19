package lake.graphics;

public abstract class AbstractVertexBuffer implements Disposable {
    public int maxQuads;
    public int vertexDataSize;

    public AbstractVertexBuffer(int maxQuads, int vertexDataSize) {
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

    @Override
    public abstract void dispose();
}
