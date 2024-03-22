package lake.graphics.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

public class LVKGenericBuffer {
    public long buffer;
    public VkBufferCreateInfo bufferInfo;

    public long pMemory;

    public LVKGenericBuffer(long buffer, VkBufferCreateInfo bufferInfo, long pMemory) {
        this.buffer = buffer;
        this.bufferInfo = bufferInfo;
        this.pMemory = pMemory;
    }

    public long getpMemory() {
        return pMemory;
    }

    public void mapAndUpload(VkDevice device, PointerBuffer data, float[] floats){

        vkMapMemory(device, getpMemory(), 0, bufferInfo.size(), 0, data);
        {
            ByteBuffer buffer = data.getByteBuffer(0, (int) bufferInfo.size());

            for(float f : floats){
                buffer.putFloat(f);
            }
        }
        vkUnmapMemory(device, getpMemory());
    }
    public void mapAndUpload(VkDevice device, PointerBuffer data, int[] ints){

        vkMapMemory(device, getpMemory(), 0, bufferInfo.size(), 0, data);
        {
            ByteBuffer buffer = data.getByteBuffer(0, (int) bufferInfo.size());

            for(int i : ints){
                buffer.putInt(i);
            }
        }
        vkUnmapMemory(device, getpMemory());
    }

}
