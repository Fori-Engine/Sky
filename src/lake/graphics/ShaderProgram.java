package lake.graphics;

import org.joml.Matrix4f;

public abstract class ShaderProgram {

    protected String vertexShaderSource = null;
    protected String fragmentShaderSource = null;

    public ShaderProgram(String vertexShaderSource, String fragmentShaderSource){
        this.vertexShaderSource = vertexShaderSource;
        this.fragmentShaderSource = fragmentShaderSource;
    }



    public abstract void prepare();

    public String getVertexShaderSource() {
        return vertexShaderSource;
    }

    public String getFragmentShaderSource() {
        return fragmentShaderSource;
    }


    public abstract void bind();
    public abstract void setFloat(String name, float value);
    public abstract void setInt(String name, int value);
    public abstract void setMatrix4f(String name, Matrix4f proj);
    public abstract void setIntArray(String name, int[] array);
    public abstract void setVector2fArray(String name, float[] array);

}
