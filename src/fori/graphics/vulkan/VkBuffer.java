package fori.graphics.vulkan;

import fori.graphics.Disposable;
import fori.graphics.Disposer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocationInfo;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkBuffer implements Disposable {
    private int sizeBytes;
    private long handle;

    private LongBuffer pBuffer;
    private PointerBuffer pAllocation;
    private VmaAllocationInfo allocationInfo;
    private VmaAllocationCreateInfo allocationCreateInfo;
    private long memory;
    private long vmaAllocator;
    private PointerBuffer mappedMemory;
    private boolean mapped;

    public VkBuffer(long vmaAllocator, int sizeBytes, int bufferUsage, int memoryUsage) {
        Disposer.add("managedResources", this);
        this.sizeBytes = sizeBytes;

        this.vmaAllocator = vmaAllocator;


        VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.create();
        bufferCreateInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
        bufferCreateInfo.size(sizeBytes);
        bufferCreateInfo.usage(bufferUsage);


        allocationCreateInfo = VmaAllocationCreateInfo.create();
        allocationCreateInfo.usage(memoryUsage);
        allocationCreateInfo.requiredFlags(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

        pBuffer = MemoryUtil.memAllocLong(1);
        pAllocation = MemoryUtil.memAllocPointer(1);


        allocationInfo = VmaAllocationInfo.create();

        vmaCreateBuffer(vmaAllocator, bufferCreateInfo, allocationCreateInfo, pBuffer, pAllocation, allocationInfo);
        memory = allocationInfo.deviceMemory();

        handle = pBuffer.get();
    }

    public ByteBuffer map(){
        if(mapped) throw new RuntimeException("VkBuffer " + handle + " can not be mapped again");

        mapped = true;

        mappedMemory = MemoryUtil.memAllocPointer(1);
        vmaMapMemory(vmaAllocator, pAllocation.get(0), mappedMemory);
        return mappedMemory.getByteBuffer(sizeBytes);
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public void unmap(){
        mapped = false;

        vmaUnmapMemory(vmaAllocator, pAllocation.get(0));
        MemoryUtil.memFree(mappedMemory);
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
