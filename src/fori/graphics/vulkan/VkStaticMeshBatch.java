package fori.graphics.vulkan;

import fori.Logger;
import fori.graphics.*;
import fori.graphics.aurora.StaticMeshBatch;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;

import static fori.graphics.vulkan.VkRenderer.UINT64_MAX;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class VkStaticMeshBatch extends StaticMeshBatch {
    private VkPipeline pipeline;
    private VkCommandBuffer commandBuffer;
    private long stagingTransferFence;
    private VkQueue graphicsQueue;
    private VkDevice device;
    private Buffer stagingVertexBuffer, stagingIndexBuffer;

    public VkStaticMeshBatch(Ref ref,
                             ShaderProgram shaderProgram,
                             int framesInFlight,
                             long commandPool,
                             VkQueue graphicsQueue,
                             VkDevice device,
                             VkPipeline pipeline,
                             int maxVertices,
                             int maxIndices) {
        super(maxVertices, maxIndices, shaderProgram);
        this.graphicsQueue = graphicsQueue;
        this.device = device;
        this.pipeline = pipeline;

        try(MemoryStack stack = stackPush()) {

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffers = stack.mallocPointer(1);

            if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                throw new RuntimeException(Logger.error(VkStaticMeshBatch.class, "Failed to create per-RenderCommand command buffer"));
            }

            commandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), device);

            LongBuffer pFence = stack.mallocLong(1);

            VkFenceCreateInfo transferFenceCreateInfo = VkFenceCreateInfo.calloc(stack);
            transferFenceCreateInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            vkCreateFence(device, transferFenceCreateInfo, null, pFence);

            stagingTransferFence = pFence.get(0);
        }



        stagingVertexBuffer = Buffer.newBuffer(
                ref,
                Attributes.getSize(this.shaderProgram.getAttributes()) * Float.BYTES * maxVertexCount,
                Buffer.Usage.VertexBuffer,
                Buffer.Type.CPUGPUShared,
                true
        );
        stagingIndexBuffer = Buffer.newBuffer(
                ref,
                maxIndexCount * Integer.BYTES,
                Buffer.Usage.IndexBuffer,
                Buffer.Type.CPUGPUShared,
                true
        );
        vertexBuffer = Buffer.newBuffer(
                ref,
                Attributes.getSize(this.shaderProgram.getAttributes()) * Float.BYTES * maxVertexCount,
                Buffer.Usage.VertexBuffer,
                Buffer.Type.GPULocal,
                false
        );
        indexBuffer = Buffer.newBuffer(
                ref,
                maxIndexCount * Integer.BYTES,
                Buffer.Usage.IndexBuffer,
                Buffer.Type.GPULocal,
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



    @Override
    public Buffer getDefaultVertexBuffer() {
        return stagingVertexBuffer;
    }

    @Override
    public Buffer getDefaultIndexBuffer() {
        return stagingIndexBuffer;
    }



    public VkPipeline getPipeline() {
        return pipeline;
    }

    public long getStagingTransferFence() {
        return stagingTransferFence;
    }

    @Override
    public void updateMeshBatch(int vertexCount, int indexCount) {
        vkResetFences(device, stagingTransferFence);

        try(MemoryStack stack = stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                throw new RuntimeException(Logger.error(VkStaticMeshBatch.class, "Failed to start recording command buffer"));
            }


            VkBufferCopy.Buffer vertexBufferCopy = VkBufferCopy.calloc(1, stack);
            vertexBufferCopy.size((long) vertexCount * Attributes.getSize(shaderProgram.getAttributes()) * Float.BYTES);
            vertexBufferCopy.srcOffset(0);
            vertexBufferCopy.dstOffset((long) this.vertexCount * Attributes.getSize(shaderProgram.getAttributes()) * Float.BYTES);


            vkCmdCopyBuffer(commandBuffer, ((VkBuffer) stagingVertexBuffer).getHandle(), ((VkBuffer) vertexBuffer).getHandle(), vertexBufferCopy);


            VkBufferCopy.Buffer indexBufferCopy = VkBufferCopy.calloc(1, stack);
            indexBufferCopy.size((long) indexCount * Integer.BYTES);
            indexBufferCopy.srcOffset(0);
            indexBufferCopy.dstOffset((long) this.indexCount * Integer.BYTES);
            vkCmdCopyBuffer(commandBuffer, ((VkBuffer) stagingIndexBuffer).getHandle(), ((VkBuffer) indexBuffer).getHandle(), indexBufferCopy);


            this.vertexCount += vertexCount;
            this.indexCount += indexCount;

            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                throw new RuntimeException(Logger.error(VkStaticMeshBatch.class, "Failed to finish recording command buffer"));
            }

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(stack.pointers(commandBuffer));

            if(vkQueueSubmit(graphicsQueue, submitInfo, stagingTransferFence) != VK_SUCCESS) {
                throw new RuntimeException(Logger.error(VkStaticMeshBatch.class, "Failed to submit command buffer"));
            }

            vkWaitForFences(device, stagingTransferFence, true, UINT64_MAX);


        }
    }
}
