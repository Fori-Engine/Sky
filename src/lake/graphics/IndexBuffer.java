package lake.graphics;

public abstract class IndexBuffer implements Disposable {
    public int maxQuads;
    public int indicesPerQuad;
    public int indexSizeBytes;
    public IndexBuffer(int maxQuads, int indicesPerQuad, int indexSizeBytes) {
        this.maxQuads = maxQuads;
        this.indicesPerQuad = indicesPerQuad;
        this.indexSizeBytes = indexSizeBytes;

        Disposer.add("managedResources", this);
    }

    public int getMaxQuads() {
        return maxQuads;
    }
}
