package fori.graphics.vulkan;

import fori.Logger;
import fori.graphics.*;
import fori.graphics.StaticMeshBatch;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class VulkanStaticMeshBatch extends StaticMeshBatch {
    private VkCommandBuffer commandBuffer;
    private VulkanFence stagingTransferFence;
    private VkQueue graphicsQueue;
    private VkDevice device;
    private Buffer stagingVertexBuffer, stagingIndexBuffer;
    private Disposable parent;

    public VulkanStaticMeshBatch(Disposable parent,
                                 ShaderProgram shaderProgram,
                                 int framesInFlight,
                                 VulkanCommandPool commandPool,
                                 VkQueue graphicsQueue,
                                 VkDevice device,
                                 int maxVertexCount,
                                 int maxIndexCount,
                                 int maxTransformCount) {
        super(maxVertexCount, maxIndexCount, maxTransformCount, shaderProgram);
        this.parent = parent;
        this.graphicsQueue = graphicsQueue;
        this.device = device;

        try(MemoryStack stack = stackPush()) {

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool.getHandle());
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffers = stack.mallocPointer(1);

            if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                throw new RuntimeException(Logger.error(VulkanStaticMeshBatch.class, "Failed to create per-RenderCommand command buffer"));
            }

            commandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), device);

            stagingTransferFence = new VulkanFence(parent, VulkanRuntime.getCurrentDevice(), 0);
        }



        stagingVertexBuffer = Buffer.newBuffer(
                parent,
                VertexAttributes.getSize(this.shaderProgram.getAttributes().get()) * Float.BYTES * this.maxVertexCount,
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
                VertexAttributes.getSize(this.shaderProgram.getAttributes().get()) * Float.BYTES * this.maxVertexCount,
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

        transformsBuffers = new Buffer[framesInFlight];
        cameraBuffers = new Buffer[framesInFlight];

        for (int i = 0; i < framesInFlight; i++) {
            transformsBuffers[i] = Buffer.newBuffer(
                    parent,
                    SizeUtil.MATRIX_SIZE_BYTES * maxTransformCount,
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
    public void submitMesh(Mesh mesh, MeshUploader meshUploader) {

        getDefaultVertexBuffer().get().clear();
        getDefaultIndexBuffer().get().clear();

        mesh.put(
                meshUploader,
                getVertexCount(),
                getShaderProgram(),
                getDefaultVertexBuffer().get(),
                getDefaultIndexBuffer().get()
        );

        updateMeshBatch(
                mesh.getVertexCount(),
                mesh.getIndexCount()
        );
    }

    @Override
    public Buffer getDefaultVertexBuffer() {
        return stagingVertexBuffer;
    }

    @Override
    public Buffer getDefaultIndexBuffer() {
        return stagingIndexBuffer;
    }

    @Override
    public void finish() {
        super.finish();

        stagingVertexBuffer.dispose();
        stagingIndexBuffer.dispose();
        stagingTransferFence.dispose();

        parent.remove(stagingVertexBuffer);
        parent.remove(stagingIndexBuffer);
        parent.remove(stagingTransferFence);
    }



    @Override
    public void updateMeshBatch(int vertexCount, int indexCount) {
        vkResetFences(device, stagingTransferFence.getHandle());

        try(MemoryStack stack = stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                throw new RuntimeException(Logger.error(VulkanStaticMeshBatch.class, "Failed to start recording command buffer"));
            }


            VkBufferCopy.Buffer vertexBufferCopy = VkBufferCopy.calloc(1, stack);
            vertexBufferCopy.size((long) vertexCount * VertexAttributes.getSize(shaderProgram.getAttributes().get()) * Float.BYTES);
            vertexBufferCopy.srcOffset(0);
            vertexBufferCopy.dstOffset((long) this.vertexCount * VertexAttributes.getSize(shaderProgram.getAttributes().get()) * Float.BYTES);

            vkCmdCopyBuffer(commandBuffer, ((VulkanBuffer) stagingVertexBuffer).getHandle(), ((VulkanBuffer) vertexBuffer).getHandle(), vertexBufferCopy);


            VkBufferCopy.Buffer indexBufferCopy = VkBufferCopy.calloc(1, stack);
            indexBufferCopy.size((long) indexCount * Integer.BYTES);
            indexBufferCopy.srcOffset(0);
            indexBufferCopy.dstOffset((long) this.indexCount * Integer.BYTES);

            vkCmdCopyBuffer(commandBuffer, ((VulkanBuffer) stagingIndexBuffer).getHandle(), ((VulkanBuffer) indexBuffer).getHandle(), indexBufferCopy);


            this.vertexCount += vertexCount;
            this.indexCount += indexCount;

            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                throw new RuntimeException(Logger.error(VulkanStaticMeshBatch.class, "Failed to finish recording command buffer"));
            }

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(stack.pointers(commandBuffer));

            if(vkQueueSubmit(graphicsQueue, submitInfo, stagingTransferFence.getHandle()) != VK_SUCCESS) {
                throw new RuntimeException(Logger.error(VulkanStaticMeshBatch.class, "Failed to submit command buffer"));
            }

            vkWaitForFences(device, stagingTransferFence.getHandle(), true, VulkanUtil.UINT64_MAX);


        }
    }
}
