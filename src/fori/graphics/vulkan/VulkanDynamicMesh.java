package fori.graphics.vulkan;

import fori.graphics.*;
import fori.graphics.DynamicMesh;
import java.nio.ByteBuffer;

public class VulkanDynamicMesh extends DynamicMesh {
    public VulkanDynamicMesh(Disposable parent,
                             ShaderProgram shaderProgram,
                             int framesInFlight,
                             int maxVertexCount,
                             int maxIndexCount,
                             int maxCameraCount) {

        super(maxVertexCount, maxIndexCount, maxCameraCount, shaderProgram);
        vertexBuffer = Buffer.newBuffer(
                parent,
                VertexAttributes.getSize(shaderProgram.getShaderMap().get(ShaderType.Vertex).getVertexAttributes()) * Float.BYTES * this.maxVertexCount,
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
        sceneDescBuffers = new Buffer[framesInFlight];

        for (int i = 0; i < framesInFlight; i++) {
            transformsBuffers[i] = Buffer.newBuffer(
                    parent,
                    SizeUtil.MATRIX_SIZE_BYTES,
                    Buffer.Usage.ShaderStorageBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );

            sceneDescBuffers[i] = Buffer.newBuffer(
                    parent,
                    SizeUtil.MATRIX_SIZE_BYTES * maxCameraCount * 2,
                    Buffer.Usage.ShaderStorageBuffer,
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
