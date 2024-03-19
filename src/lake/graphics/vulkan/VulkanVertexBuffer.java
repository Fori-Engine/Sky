package lake.graphics.vulkan;

import lake.graphics.VertexBuffer;
import lake.graphics.Disposer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanVertexBuffer extends VertexBuffer {

    private VkDevice device;
    private VkPhysicalDevice physicalDevice;
    private long vertexBufferMemory;
    private PointerBuffer data;
    private VulkanGenericBuffer buffer;

    public VulkanVertexBuffer(int maxQuads, int vertexDataSize) {
        super(maxQuads, vertexDataSize);



    }

    public VkDevice getDevice() {
        return device;
    }

    public void setDevice(VkDevice device) {
        this.device = device;
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

    public long getVertexBufferMemory() {
        return vertexBufferMemory;
    }

    @Override
    public int getNumOfVertices() {
        return 0;
    }


    @Override
    public void build() {


        int vertexSizeBytes = 5 * Float.BYTES;

        data = MemoryUtil.memAllocPointer(1);
        LongBuffer pVertexBufferMemory = MemoryUtil.memAllocLong(1);

        buffer = FastVK.createBuffer(
                device,
                physicalDevice,
                vertexSizeBytes * 3,
                VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                pVertexBufferMemory
        );
        vertexBufferMemory = pVertexBufferMemory.get(0);












    }

    public long getVertexBuffer() {
        return buffer.buffer;
    }

    @Override
    public void dispose() {
        MemoryUtil.memFree(data);

    }

}
