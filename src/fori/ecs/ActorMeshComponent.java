package fori.ecs;

import fori.graphics.*;

import java.nio.ByteBuffer;

public class ActorMeshComponent {
    public Buffer[] transformsBuffers;
    public Buffer[] sceneDescBuffers;
    public ShaderProgram shaderProgram;
    public int maxVertexCount;
    public int maxIndexCount;
    public int vertexCount;
    public int indexCount;
    public Buffer vertexBuffer;
    public Buffer indexBuffer;
    public boolean finalized;

    public ActorMeshComponent(Disposable parent, Renderer renderer, int maxVertexCount, int maxIndexCount, ShaderProgram shaderProgram) {
        this.maxVertexCount = maxVertexCount;
        this.maxIndexCount = maxIndexCount;
        this.shaderProgram = shaderProgram;

        vertexBuffer = Buffer.newBuffer(
                parent,
                shaderProgram.getVertexAttributesSize() * Float.BYTES * this.maxVertexCount,
                Buffer.Usage.VertexBuffer,
                Buffer.Type.CPUGPUShared,
                false
        );
        indexBuffer = Buffer.newBuffer(
                parent,
                this.maxIndexCount * Integer.BYTES,
                Buffer.Usage.IndexBuffer,
                Buffer.Type.CPUGPUShared,
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
                    shaderProgram.getDescriptorByName("sceneDesc").getSizeBytes(),
                    Buffer.Usage.ShaderStorageBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );
        }
    }



    public void setMesh(MeshData meshData, EntityShaderIndex entityShaderIndex) {
        ByteBuffer vertexBufferData = vertexBuffer.get();
        vertexBufferData.clear();

        ByteBuffer indexBufferData = indexBuffer.get();
        indexBufferData.clear();

        entityShaderIndex.upload(meshData, shaderProgram, vertexBufferData, indexBufferData, 0);

        this.vertexCount = meshData.getVertexCount();
        this.indexCount = meshData.getIndexCount();
        finalized = true;
    }


}
