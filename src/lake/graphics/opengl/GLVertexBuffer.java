package lake.graphics.opengl;

import lake.graphics.Disposable;
import lake.graphics.Disposer;

import static org.lwjgl.opengl.GL46.*;

/***
 * Represents an OpenGL Vertex Buffer. This is a Disposable OpenGL object and will be disposed by the Window.
 * This class also manages the Index Buffer automatically
 */
public class GLVertexBuffer extends lake.graphics.VertexBuffer implements Disposable {
    public int myVbo;
    private int numOfVertices;

    public GLVertexBuffer(int maxQuads, int vertexDataSize) {
        super(maxQuads, vertexDataSize);
        Disposer.add("managedResources", this);

        build();
    }

    public int getNumOfVertices() {
        return numOfVertices;
    }


    /***
     */
    public void build() {
        myVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, myVbo);
        glBufferData(GL_ARRAY_BUFFER, maxQuads * 4 * vertexDataSize * Float.BYTES, GL_DYNAMIC_DRAW);
        numOfVertices = maxQuads * 4;
    }

    @Override
    public void dispose() {
        glDeleteBuffers(myVbo);
    }

}
