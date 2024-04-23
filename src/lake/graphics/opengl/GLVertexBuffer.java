package lake.graphics.opengl;

import lake.graphics.VertexBuffer;

import static org.lwjgl.opengl.GL46.*;

public class GLVertexBuffer extends VertexBuffer {
    public int myVbo;


    public GLVertexBuffer(int maxQuads, int vertexDataSize) {
        super(maxQuads, vertexDataSize);

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
