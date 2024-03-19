package lake.graphics.vulkan;

import org.lwjgl.vulkan.VkBufferCreateInfo;

public class VulkanGenericBuffer {
    public long buffer;
    public VkBufferCreateInfo bufferInfo;

    public VulkanGenericBuffer(long buffer, VkBufferCreateInfo bufferInfo) {
        this.buffer = buffer;
        this.bufferInfo = bufferInfo;
    }

}
