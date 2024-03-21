package lake.graphics.vulkan;

import org.lwjgl.vulkan.VkBufferCreateInfo;

public class LVKGenericBuffer {
    public long buffer;
    public VkBufferCreateInfo bufferInfo;

    public LVKGenericBuffer(long buffer, VkBufferCreateInfo bufferInfo) {
        this.buffer = buffer;
        this.bufferInfo = bufferInfo;
    }

}
