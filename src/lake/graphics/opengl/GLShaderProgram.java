package lake.graphics.opengl;

import lake.graphics.*;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL46.*;
/***
 * Represents an OpenGL Shader Program. This is a Disposable OpenGL object and will be disposed by the Window.
 */
public class GLShaderProgram extends ShaderProgram {

    private int shaderProgram;

    /***
     * Create a Shader from the specified Vertex and Fragment Shader sources
     * @param vertexShaderSource
     * @param fragmentShaderSource
     */
    public GLShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        super(vertexShaderSource, fragmentShaderSource);
        Disposer.add("managedResources", this);
    }

    @Override
    public ByteBuffer[] mapUniformBuffer(ShaderResource resource) {
        return new ByteBuffer[0];
    }

    @Override
    public void unmapUniformBuffer(ShaderResource resource, ByteBuffer[] byteBuffers) {

    }

    @Override
    public void updateEntireSampler2DArrayWithOnly(ShaderResource resource, Texture2D texture) {

    }

    @Override
    public void updateSampler2DArray(ShaderResource resource, int index, Texture2D texture2D) {

    }


    /***
     * Compiles and Links the shader
     */

    public void prepare(){

        int vertexShaderProg = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShaderProg, vertexShaderSource);
        glCompileShader(vertexShaderProg);
        String vertexShaderInfoLog = glGetShaderInfoLog(vertexShaderProg);
        if(!vertexShaderInfoLog.isEmpty()){
            throw new RuntimeException("Vertex Shader: " + vertexShaderInfoLog);
        }


        int fragmentShaderProg = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShaderProg, fragmentShaderSource);
        glCompileShader(fragmentShaderProg);
        String fragmentShaderInfoLog = glGetShaderInfoLog(fragmentShaderProg);
        if(!fragmentShaderInfoLog.isEmpty()){
            throw new RuntimeException("Fragment Shader: " + fragmentShaderInfoLog);
        }


        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShaderProg);
        glAttachShader(shaderProgram, fragmentShaderProg);

        glDeleteShader(vertexShaderProg);
        glDeleteShader(fragmentShaderProg);

        glLinkProgram(shaderProgram);
    }


    public void bind(){
        glUseProgram(shaderProgram);
    }

    public int getShaderProgram() {
        return shaderProgram;
    }

    public void setFloat(String name, float value){
        int location = glGetUniformLocation(shaderProgram, name);
        glUniform1f(location, value);
    }

    public void setInt(String name, int value){
        int location = glGetUniformLocation(shaderProgram, name);
        glUniform1i(location, value);
    }

    public void setMatrix4f(String name, Matrix4f proj) {
        int location = glGetUniformLocation(shaderProgram, name);

        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);
        proj.get(floatBuffer);

        glUniformMatrix4fv(location, false, floatBuffer);
    }

    public void setIntArray(String name, int[] array) {
        int location = glGetUniformLocation(shaderProgram, name);
        glUniform1iv(location, array);
    }
    public void setVector2fArray(String name, float[] array) {
        int location = glGetUniformLocation(shaderProgram, name);
        glUniform2fv(location, array);
    }

    @Override
    public void dispose() {
        glDeleteProgram(shaderProgram);
    }
}
