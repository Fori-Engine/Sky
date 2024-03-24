package lake.graphics.opengl;

import lake.graphics.Disposable;
import lake.graphics.Disposer;

import static org.lwjgl.opengl.GL46.*;

/***
 * Represents an OpenGL Vertex Array. This is a Disposable OpenGL object and will be disposed by the Window.
 */
public class GLVertexArray implements Disposable {
    private int myVao;

    private GLVertexAttribute[] vertexAttributes;
    private int stride = 0;




    public GLVertexArray() {
        Disposer.add("managedResources", this);
        myVao = glGenVertexArrays();
    }

    public void bind(){
        glBindVertexArray(myVao);
    }


    public void build() {

        int pointer = 0;

        for(GLVertexAttribute vertexAttribute : vertexAttributes){

            glVertexAttribPointer(
                    vertexAttribute.index,
                    vertexAttribute.size,
                    GL_FLOAT,
                    vertexAttribute.normalized,
                    stride,
                    pointer
            );

            glEnableVertexAttribArray(vertexAttribute.index);

            pointer += vertexAttribute.size * Float.BYTES;
        }

    }

    public void setVertexAttributes(GLVertexAttribute... vertexAttributes) {
        this.vertexAttributes = vertexAttributes;
        for(GLVertexAttribute vertexAttribute : vertexAttributes){
            stride += vertexAttribute.size * Float.BYTES;
        }
    }

    public int getStride() {
        return stride;
    }

    @Override
    public void dispose() {
        glDeleteVertexArrays(myVao);
    }

    public GLVertexAttribute[] getVertexAttributes() {
        return vertexAttributes;
    }
}
