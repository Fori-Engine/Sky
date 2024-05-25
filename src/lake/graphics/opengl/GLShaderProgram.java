package lake.graphics.opengl;

import lake.graphics.*;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import static org.lwjgl.opengl.GL46.*;
/***
 * Represents an OpenGL Shader Program. This is a Disposable OpenGL object and will be disposed by the Window.
 */
public class GLShaderProgram extends ShaderProgram {

    private int shaderProgram;
    private boolean ubosInitialized;
    private HashMap<Integer, Integer> bindingToUBOBlockIndex = new HashMap<>();
    private HashMap<Integer, Integer> bindingToUniformLocation = new HashMap<>();
    private HashMap<Integer, Integer> uniformBuffers = new HashMap<>();


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
        int bufferID = uniformBuffers.get(resource.binding);
        glBindBuffer(GL_UNIFORM_BUFFER, bufferID);
        return new ByteBuffer[]{glMapBuffer(GL_UNIFORM_BUFFER, GL_READ_WRITE)};
    }

    @Override
    public void unmapUniformBuffer(ShaderResource resource, ByteBuffer[] byteBuffers) {
        glUnmapBuffer(GL_UNIFORM_BUFFER);
    }

    @Override
    public void updateEntireSampler2DArrayWithOnly(ShaderResource resource, Texture2D texture) {
        GLTexture2D glTexture2D = (GLTexture2D) texture;

        for (int i = 0; i < resource.count; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glTexture2D.bind();
            glTexture2D.setSlot(i);
        }
    }

    @Override
    public void updateSampler2DArray(ShaderResource resource, int index, Texture2D tex) {
        GLTexture2D glTexture2D = (GLTexture2D) tex;
        glActiveTexture(GL_TEXTURE0 + index);
        glTexture2D.bind();
        glTexture2D.setSlot(index);
    }

    @Override
    public void addResource(ShaderResource resource) {
        super.addResource(resource);


        try(MemoryStack stack = MemoryStack.stackPush()){


            if(resource.type == ShaderResource.Type.UniformBuffer) {

                IntBuffer n = stack.callocInt(1);
                glGetProgramiv(shaderProgram, GL_ACTIVE_UNIFORM_BLOCKS, n);
                int numOfUniformBlocks = n.get(0);
                for (int i = 0; i < numOfUniformBlocks; i++) {
                    IntBuffer bindingPoint = stack.callocInt(1);
                    glGetActiveUniformBlockiv(shaderProgram, i, GL_UNIFORM_BLOCK_BINDING, bindingPoint);
                    if (bindingPoint.get(0) == resource.binding) {
                        bindingToUBOBlockIndex.put(resource.binding, i);

                        int id = glGenBuffers();
                        glBindBuffer(GL_UNIFORM_BUFFER, id);
                        glBufferData(GL_UNIFORM_BUFFER, resource.sizeBytes, GL_DYNAMIC_DRAW);
                        glBindBufferBase(GL_UNIFORM_BUFFER, resource.binding, id);
                        glUniformBlockBinding(shaderProgram, i, resource.binding);

                        uniformBuffers.put(resource.binding, id);
                    }
                }
            }
            else {

                IntBuffer n = stack.mallocInt(1);
                glGetProgramiv(shaderProgram, GL_ACTIVE_UNIFORMS, n);
                int numOfUniform = n.get(0);
                for (int i = 0; i < numOfUniform; i++) {
                    IntBuffer bindingPoint = stack.mallocInt(1);

                    glGetUniformiv(shaderProgram, i, bindingPoint);

                    IntBuffer length = stack.mallocInt(1);
                    IntBuffer size = stack.mallocInt(1);
                    IntBuffer type = stack.mallocInt(1);
                    ByteBuffer name = stack.malloc(1);

                    glGetActiveUniform(shaderProgram, i, length, size, type, name);

                    if(bindingPoint.get() == resource.binding){
                        int location = glGetUniformLocation(shaderProgram, name);
                        bindingToUniformLocation.put(resource.binding, location);
                    }








                }
            }
        }

    }

    public void createUniformBufferMemory(){

        try(MemoryStack stack = MemoryStack.stackPush()) {

















            for (ShaderResource shaderResource : resources) {
                if (shaderResource.type == ShaderResource.Type.UniformBuffer) {

                }
            }


        }


    }

    public boolean isUbosInitialized() {
        return ubosInitialized;
    }

    public void setUbosInitialized(boolean ubosInitialized) {
        this.ubosInitialized = ubosInitialized;
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
