package lake.graphics;

public abstract class VertexBuffer {
    protected int vertexDataSize;
    public abstract void build();

    public int getVertexDataSize() {
        return vertexDataSize;
    }
}
