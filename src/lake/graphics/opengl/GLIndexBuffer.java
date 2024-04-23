package lake.graphics.opengl;

import lake.graphics.IndexBuffer;

import static org.lwjgl.opengl.GL46.*;

public class GLIndexBuffer extends IndexBuffer {
    public int myEbo;
    public GLIndexBuffer(int maxQuads, int indicesPerQuad, int indexSizeBytes){
        super(maxQuads, indicesPerQuad, indexSizeBytes);

        myEbo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, myEbo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, maxQuads * indicesPerQuad * indexSizeBytes, GL_DYNAMIC_DRAW);
    }

    @Override
    public void dispose() {
        glDeleteBuffers(myEbo);
    }
}
