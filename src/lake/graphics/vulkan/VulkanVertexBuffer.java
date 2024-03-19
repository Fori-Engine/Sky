package lake.graphics.vulkan;

import lake.graphics.VertexBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanVertexBuffer extends VertexBuffer {

    private VkDeviceWithIndices deviceWithIndices;
    private VkPhysicalDevice physicalDevice;
    private long vertexBufferMemory, stagingBufferMemory;
    private PointerBuffer data;
    private VulkanGenericBuffer buffer;
    private VulkanGenericBuffer stagingBuffer;
    private VkQueue graphicsQueue;
    private long commandPool;



    public VulkanVertexBuffer(int maxQuads, int vertexDataSize) {
        super(maxQuads, vertexDataSize);
    }

    public VkDeviceWithIndices getDeviceWithIndices() {
        return deviceWithIndices;
    }

    public void setDeviceWithIndices(VkDeviceWithIndices deviceWithIndices) {
        this.deviceWithIndices = deviceWithIndices;
    }

    public long getCommandPool() {
        return commandPool;
    }

    public VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public void setGraphicsQueue(VkQueue graphicsQueue) {
        this.graphicsQueue = graphicsQueue;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public void setPhysicalDevice(VkPhysicalDevice physicalDevice) {
        this.physicalDevice = physicalDevice;
    }

    public PointerBuffer getData() {
        return data;
    }

    public VkBufferCreateInfo getBufferInfo() {
        return buffer.bufferInfo;
    }


    @Override
    public int getNumOfVertices() {
        return 0;
    }


    @Override
    public void build() {

        int vertexSizeBytes = 5 * Float.BYTES;
        int verticesSizeBytes = vertexSizeBytes * 3;
        data = MemoryUtil.memAllocPointer(1);



        LongBuffer pStagingBufferMemory = MemoryUtil.memAllocLong(1);
        stagingBuffer = FastVK.createBuffer(
                deviceWithIndices.device,
                physicalDevice,
                verticesSizeBytes,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                pStagingBufferMemory
        );
        stagingBufferMemory = pStagingBufferMemory.get(0);


        LongBuffer pVertexBufferMemory = MemoryUtil.memAllocLong(1);
        buffer = FastVK.createBuffer(
                deviceWithIndices.device,
                physicalDevice,
                verticesSizeBytes,
                VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                pVertexBufferMemory
        );
        vertexBufferMemory = pVertexBufferMemory.get(0);


        commandPool = FastVK.createCommandPool(deviceWithIndices);
        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.create();
        allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
        allocInfo.commandPool(commandPool);
        allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
        allocInfo.commandBufferCount(1);

        PointerBuffer pCommandBuffer = MemoryUtil.memAllocPointer(1);
        vkAllocateCommandBuffers(deviceWithIndices.device, allocInfo, pCommandBuffer);
        VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(), deviceWithIndices.device);


        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.create();
        beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
        beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

        vkBeginCommandBuffer(commandBuffer, beginInfo);


        VkBufferCopy.Buffer copyRegion = VkBufferCopy.create(1);;
        copyRegion.srcOffset(0);
        copyRegion.dstOffset(0);
        copyRegion.size(verticesSizeBytes);
        vkCmdCopyBuffer(commandBuffer, stagingBuffer.buffer, buffer.buffer, copyRegion);
        vkEndCommandBuffer(commandBuffer);

        VkSubmitInfo submitInfo = VkSubmitInfo.create();
        {
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(pCommandBuffer);
        }


        if(vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE) != VK_SUCCESS) {
            throw new RuntimeException("Failed to submit copy command buffer");
        }

        vkQueueWaitIdle(graphicsQueue);
        vkFreeCommandBuffers(deviceWithIndices.device, commandPool, pCommandBuffer);



    }

    public long getVertexBuffer() {
        return buffer.buffer;
    }

    public long getStagingBuffer(){
        return stagingBuffer.buffer;
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

    }

}
