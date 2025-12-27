package fori.ecs;

import fori.graphics.*;

public class EnvironmentMeshComponent {
    public Buffer[] transformsBuffers;
    public Buffer[] sceneDescBuffers;
    public Buffer vertexBuffer;
    public Buffer indexBuffer;
    public ShaderProgram shaderProgram;
    public int vertexCount;
    public int indexCount;
    public int maxVertexCount;
    public int maxIndexCount;
    public int maxTransformCount;
    public boolean finalized;

    private Buffer stagingVertexBuffer, stagingIndexBuffer;
    private Disposable parent;

    public EnvironmentMeshComponent(Disposable parent, Renderer renderer, int maxVertexCount, int maxIndexCount, int maxTransformCount, ShaderProgram shaderProgram) {
        this.parent = parent;
        this.maxIndexCount = maxIndexCount;
        this.maxVertexCount = maxVertexCount;
        this.maxTransformCount = maxTransformCount;
        this.shaderProgram = shaderProgram;


        stagingVertexBuffer = Buffer.newBuffer(
                parent,
                VertexAttributes.getSize(this.shaderProgram.getShaderMap().get(ShaderType.Vertex).getVertexAttributes()) * Float.BYTES * this.maxVertexCount,
                Buffer.Usage.VertexBuffer,
                Buffer.Type.CPUGPUShared,
                true
        );
        stagingIndexBuffer = Buffer.newBuffer(
                parent,
                this.maxIndexCount * Integer.BYTES,
                Buffer.Usage.IndexBuffer,
                Buffer.Type.CPUGPUShared,
                true
        );

        vertexBuffer = Buffer.newBuffer(
                parent,
                VertexAttributes.getSize(shaderProgram.getShaderMap().get(ShaderType.Vertex).getVertexAttributes()) * Float.BYTES * this.maxVertexCount,
                Buffer.Usage.VertexBuffer,
                Buffer.Type.GPULocal,
                false
        );
        indexBuffer = Buffer.newBuffer(
                parent,
                this.maxIndexCount * Integer.BYTES,
                Buffer.Usage.IndexBuffer,
                Buffer.Type.GPULocal,
                false
        );

        transformsBuffers = new Buffer[renderer.getMaxFramesInFlight()];
        sceneDescBuffers = new Buffer[renderer.getMaxFramesInFlight()];

        for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
            transformsBuffers[i] = Buffer.newBuffer(
                    parent,
                    SizeUtil.MATRIX_SIZE_BYTES,
                    Buffer.Usage.ShaderStorageBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );

            sceneDescBuffers[i] = Buffer.newBuffer(
                    parent,
                    SizeUtil.SCENE_DESC_SIZE_BYTES,
                    Buffer.Usage.ShaderStorageBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );
        }
    }



    public void addMesh(Mesh mesh, MeshUploader meshUploader) {

        stagingVertexBuffer.get().clear();
        stagingIndexBuffer.get().clear();

        mesh.put(
                meshUploader,
                vertexCount,
                shaderProgram,
                stagingVertexBuffer.get(),
                stagingIndexBuffer.get()
        );

        commitMesh(
                mesh.getVertexCount(),
                mesh.getIndexCount()
        );
    }

    private void commitMesh(int vertexCount, int indexCount) {
        if(!finalized) {
            stagingVertexBuffer.copyTo(
                    vertexBuffer,
                    0,
                    this.vertexCount * VertexAttributes.getSize(shaderProgram.getShaderMap().get(ShaderType.Vertex).getVertexAttributes()) * Float.BYTES,
                    vertexCount * VertexAttributes.getSize(shaderProgram.getShaderMap().get(ShaderType.Vertex).getVertexAttributes()) * Float.BYTES
            );

            stagingIndexBuffer.copyTo(
                    indexBuffer,
                    0,
                    this.indexCount * Integer.BYTES,
                    indexCount * Integer.BYTES
            );

            this.vertexCount += vertexCount;
            this.indexCount += indexCount;
        }
        else {
            throw new RuntimeException("EnvironmentMeshComponent has already been finalized");
        }
    }

    public void close() {
        finalized = true;
        stagingVertexBuffer.disposeAll();
        stagingIndexBuffer.disposeAll();

        parent.remove(stagingVertexBuffer);
        parent.remove(stagingIndexBuffer);
    }



}
