package fori.graphics;

import java.util.LinkedList;
import java.util.List;

public abstract class RenderCommand {
    public ShaderProgram shaderProgram;

    public Buffer stagingVertexBuffer;
    public Buffer stagingIndexBuffer;
    public Buffer vertexBuffer;
    public Buffer indexBuffer;
    public Buffer[] transformsBuffer;
    public Buffer[] cameraBuffer;
    public int indexCount;
    public List<Texture> textures = new LinkedList<>();

    public RenderCommand(int framesInFlight){

    }

    public abstract Buffer getDefaultVertexBuffer();

    public abstract Buffer getDefaultIndexBuffer();

    public abstract void update();
}
