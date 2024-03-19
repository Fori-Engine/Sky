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
    private long vertexBuffer;
    private long vertexBufferMemory;
    private PointerBuffer data;
    private VkBufferCreateInfo bufferInfo;

    public VulkanVertexBuffer(int maxQuads, int vertexDataSize) {
        super(maxQuads, vertexDataSize);
        Disposer.add(this);



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
        return bufferInfo;
    }

    public long getVertexBufferMemory() {
        return vertexBufferMemory;
    }

    @Override
    public int getNumOfVertices() {
        return 0;
    }

    private int findMemoryType(int typeFilter, int properties) {

        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.create();
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);

        for(int i = 0;i < memProperties.memoryTypeCount();i++) {
            if((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }

        throw new RuntimeException("Failed to find suitable memory type");
    }
    @Override
    public void build() {


        int size = 5 * Float.BYTES;










        bufferInfo = VkBufferCreateInfo.create();
        bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);

        //VBSIZE
        bufferInfo.size((long) size * 3);
        bufferInfo.usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
        bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

        LongBuffer pVertexBuffer = MemoryUtil.memAllocLong(1);

        if(vkCreateBuffer(device, bufferInfo, null, pVertexBuffer) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create vertex buffer");
        }
        vertexBuffer = pVertexBuffer.get(0);

        VkMemoryRequirements memRequirements = VkMemoryRequirements.create();
        vkGetBufferMemoryRequirements(device, vertexBuffer, memRequirements);

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.create();
        allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
        allocInfo.allocationSize(memRequirements.size());
        allocInfo.memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(),
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT));

        LongBuffer pVertexBufferMemory = MemoryUtil.memAllocLong(1);

        if(vkAllocateMemory(device, allocInfo, null, pVertexBufferMemory) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate vertex buffer memory");
        }
        vertexBufferMemory = pVertexBufferMemory.get(0);

        vkBindBufferMemory(device, vertexBuffer, vertexBufferMemory, 0);
        data = MemoryUtil.memAllocPointer(1);
    }

    public long getVertexBuffer() {
        return vertexBuffer;
    }

    @Override
    public void dispose() {
        MemoryUtil.memFree(data);
    }

}
