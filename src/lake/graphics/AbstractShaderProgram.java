package lake.graphics;

public abstract class AbstractShaderProgram {

    String vertexShaderSource = null;
    String fragmentShaderSource = null;

    public AbstractShaderProgram(String vertexShaderSource, String fragmentShaderSource){
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
}
