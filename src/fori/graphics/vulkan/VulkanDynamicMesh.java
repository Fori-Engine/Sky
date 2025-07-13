package fori.graphics.vulkan;

import fori.graphics.*;
import fori.graphics.DynamicMesh;
import java.nio.ByteBuffer;

public class VulkanDynamicMesh extends DynamicMesh {
    public VulkanDynamicMesh(Disposable parent,
                             ShaderProgram shaderProgram,
                             int framesInFlight,
                             int maxVertexCount,
                             int maxIndexCount) {

        super(maxVertexCount, maxIndexCount, shaderProgram);
        vertexBuffer = Buffer.newBuffer(
                parent,
                VertexAttributes.getSize(shaderProgram.getAttributes().get()) * Float.BYTES * this.maxVertexCount,
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

        transformsBuffers = new Buffer[framesInFlight];
        cameraBuffers = new Buffer[framesInFlight];

        for (int i = 0; i < framesInFlight; i++) {
            transformsBuffers[i] = Buffer.newBuffer(
                    parent,
                    SizeUtil.MATRIX_SIZE_BYTES,
                    Buffer.Usage.ShaderStorageBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );

            cameraBuffers[i] = Buffer.newBuffer(
                    parent,
                    Camera.SIZE,
                    Buffer.Usage.UniformBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );
        }


    }


    @Override
    public void submit(Mesh mesh, MeshUploader meshUploader) {


        ByteBuffer vertexBufferData = getVertexBuffer().get();
        vertexBufferData.clear();

        ByteBuffer indexBufferData = getIndexBuffer().get();
        indexBufferData.clear();

        mesh.put(
                meshUploader,
                0,
                shaderProgram,
                vertexBufferData,
                indexBufferData
        );

        updateMesh(mesh.getVertexCount(), mesh.getIndexCount());
    }

    @Override
    public void updateMesh(int vertexCount, int indexCount) {
        super.updateMesh(vertexCount, indexCount);
        this.vertexCount = vertexCount;
        this.indexCount = indexCount;
    }
}
