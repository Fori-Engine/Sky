package lake.graphics.vulkan;

import lake.graphics.Disposable;
import lake.graphics.Disposer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanIndexBuffer implements Disposable {

    private VkDeviceWithIndices deviceWithIndices;
    private VkPhysicalDevice physicalDevice;
    private long indexBufferMemory, stagingBufferMemory;
    private PointerBuffer data;
    private VulkanGenericBuffer buffer;
    private VulkanGenericBuffer stagingBuffer;
    private VkQueue graphicsQueue;
    private long commandPool;

    private int indexSizeBytes;

    public VulkanIndexBuffer(int indexSizeBytes) {
        this.indexSizeBytes = indexSizeBytes;
        Disposer.add("managedResources", this);
    }

    public VkDeviceWithIndices getDeviceWithIndices() {
        return deviceWithIndices;
    }

    public void setDeviceWithIndices(VkDeviceWithIndices deviceWithIndices) {
        this.deviceWithIndices = deviceWithIndices;
    }

    public void setCommandPool(long commandPool) {
        this.commandPool = commandPool;
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




    public void build() {

        //TODO Remove hardcoded 6 indices per quad!
        int indicesSizeBytes = indexSizeBytes * 6;
        data = MemoryUtil.memAllocPointer(1);



        LongBuffer pStagingBufferMemory = MemoryUtil.memAllocLong(1);
        stagingBuffer = FastVK.createBuffer(
                deviceWithIndices.device,
                physicalDevice,
                indicesSizeBytes,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                pStagingBufferMemory
        );
        stagingBufferMemory = pStagingBufferMemory.get(0);


        LongBuffer pIndexBufferMemory = MemoryUtil.memAllocLong(1);
        buffer = FastVK.createBuffer(
                deviceWithIndices.device,
                physicalDevice,
                indicesSizeBytes,
                VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                pIndexBufferMemory
        );
        indexBufferMemory = pIndexBufferMemory.get(0);
        VkSubmitInfo submitInfo = FastVK.transfer(indicesSizeBytes, commandPool, deviceWithIndices.device, buffer.buffer, stagingBuffer.buffer);


        if(vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE) != VK_SUCCESS) {
            throw new RuntimeException("Failed to submit copy command buffer");
        }

        vkQueueWaitIdle(graphicsQueue);
    }

    public long getBuffer() {
        return buffer.buffer;
    }

    public long getStagingBuffer(){
        return stagingBuffer.buffer;
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

        vkDestroyBuffer(deviceWithIndices.device, getBuffer(), null);
        vkDestroyBuffer(deviceWithIndices.device, getStagingBuffer(), null);

        vkFreeMemory(deviceWithIndices.device, getIndexBufferMemory(), null);
        vkFreeMemory(deviceWithIndices.device, getStagingBufferMemory(), null);


    }
}
