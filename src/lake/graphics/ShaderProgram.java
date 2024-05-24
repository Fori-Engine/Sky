package lake.graphics;

import lake.graphics.opengl.GLShaderProgram;
import lake.graphics.vulkan.LVKShaderProgram;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public abstract class ShaderProgram implements Disposable {

    protected String vertexShaderSource = null;
    protected String fragmentShaderSource = null;
    protected ArrayList<ShaderResource> resources = new ArrayList<>();


    public ShaderProgram(String vertexShaderSource, String fragmentShaderSource){
        this.vertexShaderSource = vertexShaderSource;
        this.fragmentShaderSource = fragmentShaderSource;
    }

    public void addResource(ShaderResource resource){
        resources.add(resource);
    }

    public void removeResource(ShaderResource resource){
        resources.remove(resource);
    }

    public abstract ByteBuffer[] mapUniformBuffer(ShaderResource resource);
    public abstract void unmapUniformBuffer(ShaderResource resource, ByteBuffer[] byteBuffers);

    public abstract void prepare();

    public String getVertexShaderSource() {
        return vertexShaderSource;
    }

    public String getFragmentShaderSource() {
        return fragmentShaderSource;
    }



    //public abstract PointerBuffer getUniformBuffer(ShaderResource shaderResource);



    public abstract void bind();
    public abstract void setFloat(String name, float value);
    public abstract void setInt(String name, int value);
    public abstract void setMatrix4f(String name, Matrix4f proj);
    public abstract void setIntArray(String name, int[] array);
    public abstract void setVector2fArray(String name, float[] array);

    public abstract void dispose();

    public static ShaderProgram newShaderProgram(String vertexShaderSource, String fragmentShaderSource){
        if(Renderer2D.getRenderAPI() == RenderAPI.OpenGL) return new GLShaderProgram(vertexShaderSource, fragmentShaderSource);
        if(Renderer2D.getRenderAPI() == RenderAPI.Vulkan) return new LVKShaderProgram(vertexShaderSource, fragmentShaderSource);

        return null;
    }

}
