package lake.graphics.opengl;

import lake.graphics.Disposable;
import lake.graphics.Disposer;

import static org.lwjgl.opengl.GL46.*;

/***
 * Represents an OpenGL Vertex Buffer. This is a Disposable OpenGL object and will be disposed by the Window.
 * This class also manages the Index Buffer automatically
 */
public class VertexBuffer extends lake.graphics.VertexBuffer implements Disposable {
    public int myVbo;
    public int myEbo;
    private int numOfVertices;

    public VertexBuffer(int maxQuads, int vertexDataSize) {
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
        glBufferData(GL_ARRAY_BUFFER, maxQuads * 4 * vertexSizeBytes * Float.BYTES, GL_DYNAMIC_DRAW);
        numOfVertices = maxQuads * 4;

        myEbo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, myEbo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, maxQuads * 6L * Integer.BYTES, GL_DYNAMIC_DRAW);

    }

    @Override
    public void dispose() {
        glDeleteBuffers(myVbo);
        glDeleteBuffers(myEbo);
    }

}
