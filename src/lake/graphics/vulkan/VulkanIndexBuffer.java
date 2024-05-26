package lake.graphics.vulkan;

import lake.graphics.IndexBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanIndexBuffer extends IndexBuffer  {

    private long indexBufferMemory, stagingBufferMemory;
    private PointerBuffer data;
    private VulkanBuffer buffer;
    private VulkanBuffer stagingBuffer;

    private VkDevice device;

    public VulkanIndexBuffer(int maxQuads, int indicesPerQuad, int indexSizeBytes, VkDevice device, long commandPool, VkQueue graphicsQueue, VkPhysicalDevice physicalDevice) {
        super(maxQuads, indicesPerQuad, indexSizeBytes);
        this.device = device;


        int indicesSizeBytes = indexSizeBytes * indicesPerQuad * maxQuads;

        data = MemoryUtil.memAllocPointer(1);



        LongBuffer pStagingBufferMemory = MemoryUtil.memAllocLong(1);
        stagingBuffer = VulkanUtil.createBuffer(
                device,
                physicalDevice,
                indicesSizeBytes,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                pStagingBufferMemory
        );
        stagingBufferMemory = pStagingBufferMemory.get(0);


        LongBuffer pIndexBufferMemory = MemoryUtil.memAllocLong(1);
        buffer = VulkanUtil.createBuffer(
                device,
                physicalDevice,
                indicesSizeBytes,
                VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                pIndexBufferMemory
        );
        indexBufferMemory = pIndexBufferMemory.get(0);
        VkSubmitInfo submitInfo = VulkanUtil.transfer(indicesSizeBytes, commandPool, device, buffer.handle, stagingBuffer.handle);


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


    public long getIndexBufferMemory() {
        return indexBufferMemory;
    }

    public long getStagingBufferMemory() {
        return stagingBufferMemory;
    }


    @Override
    public void dispose() {
        MemoryUtil.memFree(data);

        vkDestroyBuffer(device, buffer.handle, null);
        vkDestroyBuffer(device, stagingBuffer.handle, null);

        vkFreeMemory(device, getIndexBufferMemory(), null);
        vkFreeMemory(device, getStagingBufferMemory(), null);


    }
}
