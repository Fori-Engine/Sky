package lake.graphics.vulkan;

import lake.graphics.Disposable;
import lake.graphics.Disposer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocationInfo;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_AUTO;
import static org.lwjgl.util.vma.Vma.vmaCreateBuffer;
import static org.lwjgl.vulkan.VK10.*;

public class VkBuffer implements Disposable {
    private int sizeBytes;
    private long handle;

    private LongBuffer pBuffer;
    private PointerBuffer pAllocation;
    private VmaAllocationInfo allocationInfo;
    private VmaAllocationCreateInfo allocationCreateInfo;
    private long memory;


    public VkBuffer(VkDevice device, long vmaAllocator, int sizeBytes, int bufferUsage, int memoryUsage) {
        Disposer.add("managedResources", this);
        this.sizeBytes = sizeBytes;

        VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.create();
        bufferCreateInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
        bufferCreateInfo.size(sizeBytes);
        bufferCreateInfo.usage(bufferUsage);


        allocationCreateInfo = VmaAllocationCreateInfo.create();
        allocationCreateInfo.usage(memoryUsage);

        pBuffer = MemoryUtil.memAllocLong(1);
        pAllocation = MemoryUtil.memAllocPointer(1);

        allocationInfo = VmaAllocationInfo.create();


        vmaCreateBuffer(vmaAllocator, bufferCreateInfo, allocationCreateInfo, pBuffer, pAllocation, allocationInfo);
        memory = allocationInfo.deviceMemory();



        handle = pBuffer.get();
    }

    public long getMemory() {
        return memory;
    }

    public long getHandle() {
        return handle;
    }

    @Override
    public void dispose() {

        MemoryUtil.memFree(pBuffer);
        MemoryUtil.memFree(pAllocation);

        allocationInfo.free();
        allocationCreateInfo.free();


    }
}
