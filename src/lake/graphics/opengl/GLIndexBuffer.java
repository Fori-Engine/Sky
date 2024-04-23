package lake.graphics.opengl;

import lake.graphics.Disposable;
import lake.graphics.Disposer;
import lake.graphics.IndexBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;

public class GLIndexBuffer extends IndexBuffer implements Disposable {


    public int myEbo;

    public GLIndexBuffer(int maxQuads){
        Disposer.add("managedResources", this);

        myEbo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, myEbo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, maxQuads * 6L * Integer.BYTES, GL_DYNAMIC_DRAW);
    }

    @Override
    public void dispose() {
        glDeleteBuffers(myEbo);
    }
}
