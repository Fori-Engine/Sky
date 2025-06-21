package fori.graphics.vulkan;

import fori.graphics.*;
import fori.graphics.aurora.DynamicMesh;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

public class VulkanDynamicMesh extends DynamicMesh {
    private VulkanPipeline pipeline;
    public VulkanDynamicMesh(Ref ref,
                             ShaderProgram shaderProgram,
                             int framesInFlight,
                             long commandPool,
                             VkQueue graphicsQueue,
                             VkDevice device,
                             VulkanPipeline pipeline,
                             int maxVertexCount,
                             int maxIndexCount) {

        super(maxIndexCount, maxVertexCount, shaderProgram);
        this.pipeline = pipeline;
        vertexBuffer = Buffer.newBuffer(
                ref,
                Attributes.getSize(shaderProgram.getAttributes()) * Float.BYTES * this.maxVertexCount,
                Buffer.Usage.VertexBuffer,
                Buffer.Type.CPUGPUShared,
                false
        );
        indexBuffer = Buffer.newBuffer(
                ref,
                this.maxIndexCount * Integer.BYTES,
                Buffer.Usage.IndexBuffer,
                Buffer.Type.CPUGPUShared,
                false
        );

        transformsBuffers = new Buffer[framesInFlight];
        cameraBuffers = new Buffer[framesInFlight];

        for (int i = 0; i < framesInFlight; i++) {
            transformsBuffers[i] = Buffer.newBuffer(
                    ref,
                    SizeUtil.MATRIX_SIZE_BYTES,
                    Buffer.Usage.ShaderStorageBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );

            cameraBuffers[i] = Buffer.newBuffer(
                    ref,
                    Camera.SIZE,
                    Buffer.Usage.UniformBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );
        }


    }

    public VulkanPipeline getPipeline() {
        return pipeline;
    }

    @Override
    public void updateMesh(int vertexCount, int indexCount) {
        super.updateMesh(vertexCount, indexCount);
        this.vertexCount = vertexCount;
        this.indexCount = indexCount;
    }
}
