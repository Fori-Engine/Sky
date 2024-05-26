package lake.graphics.vulkan;

import lake.graphics.VertexBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanVertexBuffer extends VertexBuffer {

    private long vertexBufferMemory, stagingBufferMemory;
    private PointerBuffer data;
    private VulkanBuffer buffer;
    private VulkanBuffer stagingBuffer;
    private VkDevice device;

    public VulkanVertexBuffer(int maxQuads, int vertexDataSize, VkDevice device, long commandPool, VkQueue graphicsQueue, VkPhysicalDevice physicalDevice) {
        super(maxQuads, vertexDataSize);
        this.device = device;

        int verticesSizeBytes = (vertexDataSize * Float.BYTES) * maxQuads * 4;
        data = MemoryUtil.memAllocPointer(1);



        LongBuffer pStagingBufferMemory = MemoryUtil.memAllocLong(1);
        stagingBuffer = FastVK.createBuffer(
                device,
                physicalDevice,
                verticesSizeBytes,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                pStagingBufferMemory
        );
        stagingBufferMemory = pStagingBufferMemory.get(0);


        LongBuffer pVertexBufferMemory = MemoryUtil.memAllocLong(1);
        buffer = FastVK.createBuffer(
                device,
                physicalDevice,
                verticesSizeBytes,
                VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                pVertexBufferMemory
        );

        numOfVertices = maxQuads * 4;

        vertexBufferMemory = pVertexBufferMemory.get(0);
        VkSubmitInfo submitInfo = FastVK.transfer(verticesSizeBytes, commandPool, device, buffer.handle, stagingBuffer.handle);

        if(vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE) != VK_SUCCESS) {
            throw new RuntimeException("Failed to submit copy command buffer");
        }

        vkQueueWaitIdle(graphicsQueue);

    }

    public PointerBuffer getMappingBuffer() {
        return data;
    }



    public VulkanBuffer getMainBuffer(){
        return buffer;
    }


    public long getVertexBufferMemory() {
        return vertexBufferMemory;
    }

    public long getStagingBufferMemory() {
        return stagingBufferMemory;
    }

    @Override
    public void dispose() {
        MemoryUtil.memFree(data);

        vkDestroyBuffer(device, buffer.handle, null);
        vkDestroyBuffer(device, stagingBuffer.handle, null);

        vkFreeMemory(device, getVertexBufferMemory(), null);
        vkFreeMemory(device, getStagingBufferMemory(), null);

    }

}
