package fori.graphics;

import java.util.ArrayList;
import java.util.List;

public abstract class RenderQueue {

    protected ShaderProgram shaderProgram;
    protected Buffer stagingVertexBuffer;
    protected Buffer stagingIndexBuffer;
    protected Buffer vertexBuffer;
    protected Buffer indexBuffer;
    protected Buffer[] transformsBuffer;
    protected Buffer[] cameraBuffer;
    protected int vertexCount;
    protected int indexCount;
    public static final int MAX_MESH_COUNT = 10;
    public static final int MAX_VERTEX_COUNT = 200000;
    public static final int MAX_INDEX_COUNT = 200000;
    protected int meshIndex;
    protected Texture[] textures = new Texture[MAX_MESH_COUNT * Material.MAX_MATERIALS * Material.SIZE];
    protected final int framesInFlight;
    protected List<Integer> pendingTextureUpdateIndices = new ArrayList<>();


    public RenderQueue(int framesInFlight){
        this.framesInFlight = framesInFlight;
    }

    public void addTexture(int textureIndex, Texture texture){
        textures[textureIndex] = texture;
        pendingTextureUpdateIndices.add(textureIndex);
    }

    public void setShaderProgram(ShaderProgram shaderProgram) {
        this.shaderProgram = shaderProgram;
    }

    public void setStagingVertexBuffer(Buffer stagingVertexBuffer) {
        this.stagingVertexBuffer = stagingVertexBuffer;
    }

    public void setStagingIndexBuffer(Buffer stagingIndexBuffer) {
        this.stagingIndexBuffer = stagingIndexBuffer;
    }

    public void setVertexBuffer(Buffer vertexBuffer) {
        this.vertexBuffer = vertexBuffer;
    }

    public void setIndexBuffer(Buffer indexBuffer) {
        this.indexBuffer = indexBuffer;
    }

    public void setTransformsBuffer(int index, Buffer transformsBuffer) {
        this.transformsBuffer[index] = transformsBuffer;
    }

    public void setCameraBuffer(int index, Buffer cameraBuffer) {
        this.cameraBuffer[index] = cameraBuffer;
    }

    public void setVertexCount(int vertexCount) {
        this.vertexCount = vertexCount;
    }

    public void setIndexCount(int indexCount) {
        this.indexCount = indexCount;
    }

    public void setMeshIndex(int meshIndex) {
        this.meshIndex = meshIndex;
    }

    public void setTextures(Texture[] textures) {
        this.textures = textures;
    }

    public ShaderProgram getShaderProgram() {
        return shaderProgram;
    }

    public Buffer getStagingVertexBuffer() {
        return stagingVertexBuffer;
    }

    public Buffer getStagingIndexBuffer() {
        return stagingIndexBuffer;
    }

    public Buffer getVertexBuffer() {
        return vertexBuffer;
    }

    public Buffer getIndexBuffer() {
        return indexBuffer;
    }

    public Buffer getTransformsBuffer(int index) {
        return transformsBuffer[index];
    }

    public Buffer getCameraBuffer(int index) {
        return cameraBuffer[index];
    }

    public int getIndexCount() {
        return indexCount;
    }

    public List<Integer> getPendingTextureUpdateIndices() {
        return pendingTextureUpdateIndices;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getFramesInFlight() {
        return framesInFlight;
    }

    public Texture[] getTextures() {
        return textures;
    }

    public void nextMesh(){
        meshIndex++;
    }

    public int getMeshIndex() {
        return meshIndex;
    }

    public int getMeshCount(){
        return meshIndex + 1;
    }

    public abstract Buffer getDefaultVertexBuffer();
    public abstract Buffer getDefaultIndexBuffer();
    public void reset(){
        meshIndex = 0;
        vertexCount = 0;
        indexCount = 0;
    }

    public void updateQueue(int vertexCount, int indexCount){

    }
}
