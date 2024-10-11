package fori.graphics;

import java.util.ArrayList;
import java.util.List;

public abstract class RenderQueue {
    public ShaderProgram shaderProgram;

    public Buffer stagingVertexBuffer;
    public Buffer stagingIndexBuffer;
    public Buffer vertexBuffer;
    public Buffer indexBuffer;
    public Buffer[] transformsBuffer;
    public Buffer[] cameraBuffer;
    public int vertexCount;
    public int indexCount;
    public static final int MAX_MESH_COUNT = 10;
    public static final int MAX_VERTEX_COUNT = 20000;
    public static final int MAX_INDEX_COUNT = 20000;

    public Texture[] textures = new Texture[MAX_MESH_COUNT];
    public final int framesInFlight;
    public List<Integer> pendingTextureUpdateIndices = new ArrayList<>();

    public RenderQueue(int framesInFlight){
        this.framesInFlight = framesInFlight;
    }

    public void addTexture(int textureIndex, Texture texture){
        textures[textureIndex] = texture;
        pendingTextureUpdateIndices.add(textureIndex);
    }

    public Texture[] getTextures() {
        return textures;
    }


    public abstract Buffer getDefaultVertexBuffer();
    public abstract Buffer getDefaultIndexBuffer();
    public void updateQueue(int vertexCount, int indexCount){

    }
}
