package lake.graphics;

import static org.lwjgl.opengl.GL46.*;

public class InstancingVertexBuffer extends VertexBuffer implements Disposable {
    public int myVbo;
    private int size;

    public InstancingVertexBuffer(int size, int vertexDataSize) {
        Disposer.add(this);
        this.size = size;
        this.vertexDataSize = vertexDataSize;
        build();
    }



    public void build() {
        myVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, myVbo);
        glBufferData(GL_ARRAY_BUFFER, size * vertexDataSize * Float.BYTES, GL_STREAM_DRAW);
    }


    @Override
    public void dispose() {
        glDeleteBuffers(myVbo);
    }
}
