package lake.graphics.vulkan;

import lake.graphics.Disposable;
import lake.graphics.Disposer;
import lake.graphics.VertexBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class LVKVertexBuffer extends VertexBuffer implements Disposable {

    private VkDeviceWithIndices deviceWithIndices;
    private VkPhysicalDevice physicalDevice;
    private long vertexBufferMemory, stagingBufferMemory;
    private PointerBuffer data;
    private LVKGenericBuffer buffer;
    private LVKGenericBuffer stagingBuffer;
    private VkQueue graphicsQueue;
    private long commandPool;



    public LVKVertexBuffer(int maxQuads, int vertexDataSize) {
        super(maxQuads, vertexDataSize);
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

    public PointerBuffer getMappingBuffer() {
        return data;
    }




    @Override
    public int getNumOfVertices() {
        return maxQuads * 4;
    }


    @Override
    public void build() {

        int verticesSizeBytes = (vertexDataSize * Float.BYTES) * maxQuads * 4;
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
                VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                pVertexBufferMemory
        );
        vertexBufferMemory = pVertexBufferMemory.get(0);
        VkSubmitInfo submitInfo = FastVK.transfer(verticesSizeBytes, commandPool, deviceWithIndices.device, buffer.handle, stagingBuffer.handle);

        if(vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE) != VK_SUCCESS) {
            throw new RuntimeException("Failed to submit copy command buffer");
        }

        vkQueueWaitIdle(graphicsQueue);
    }

    public LVKGenericBuffer getMainBuffer(){
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

        vkDestroyBuffer(deviceWithIndices.device, buffer.handle, null);
        vkDestroyBuffer(deviceWithIndices.device, stagingBuffer.handle, null);

        vkFreeMemory(deviceWithIndices.device, getVertexBufferMemory(), null);
        vkFreeMemory(deviceWithIndices.device, getStagingBufferMemory(), null);

    }

}
