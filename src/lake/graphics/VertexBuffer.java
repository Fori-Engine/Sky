package lake.graphics;

public abstract class VertexBuffer implements Disposable {
    public int maxQuads;
    public int vertexDataSize;
    public int numOfVertices;


    public VertexBuffer(int maxQuads, int vertexDataSize) {
        this.maxQuads = maxQuads;
        this.vertexDataSize = vertexDataSize;

        Disposer.add("managedResources", this);
    }

    public int getNumOfVertices(){
        return numOfVertices;
    }
    public int getMaxQuads() {
        return maxQuads;
    }
    public int getVertexDataSize() {
        return vertexDataSize;
    }
}
