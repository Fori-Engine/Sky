package lake.graphics.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

public class VulkanBuffer {
    public long handle;
    public VkBufferCreateInfo bufferInfo;

    public long pMemory;

    public VulkanBuffer(long handle, VkBufferCreateInfo bufferInfo, long pMemory) {
        this.handle = handle;
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

    public void mapAndUpload(VkDevice device, PointerBuffer data, byte[] bytes){

        vkMapMemory(device, getpMemory(), 0, bufferInfo.size(), 0, data);
        {
            ByteBuffer buffer = data.getByteBuffer(0, (int) bufferInfo.size());

            for(byte b : bytes){
                buffer.put(b);
            }
        }
        vkUnmapMemory(device, getpMemory());
    }

    public ByteBuffer mapAndGet(VkDevice device, PointerBuffer data){
        vkMapMemory(device, getpMemory(), 0, bufferInfo.size(), 0, data);
        return data.getByteBuffer(0, (int) bufferInfo.size());
    }

    public void unmap(VkDevice device){
        vkUnmapMemory(device, getpMemory());
    }

}
