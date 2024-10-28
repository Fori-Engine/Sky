package fori.graphics.vulkan;

import fori.graphics.Buffer;
import fori.graphics.Ref;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocationInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;



public class VkBuffer extends Buffer {
    private long handle;

    private LongBuffer pBuffer;
    private PointerBuffer pAllocation;
    private VmaAllocationInfo allocationInfo;
    private VmaAllocationCreateInfo allocationCreateInfo;
    private long memory;
    private PointerBuffer mappedMemory;

    public VkBuffer(Ref parent, int sizeBytes, Usage usage, Type type, boolean staging){
        super(parent, sizeBytes, usage, type, staging);



        VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.create();
        bufferCreateInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
        bufferCreateInfo.size(sizeBytes);
        int vkUsage = toVkUsageType(usage);

        if(type == Type.CPUGPUShared && staging) vkUsage |= VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
        if(type == Type.GPULocal) vkUsage |= VK_BUFFER_USAGE_TRANSFER_DST_BIT;

        bufferCreateInfo.usage(vkUsage);


        allocationCreateInfo = VmaAllocationCreateInfo.create();
        allocationCreateInfo.usage(toVkMemoryUsageType(type));
        allocationCreateInfo.requiredFlags(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

        pBuffer = MemoryUtil.memAllocLong(1);
        pAllocation = MemoryUtil.memAllocPointer(1);


        allocationInfo = VmaAllocationInfo.create();

        vmaCreateBuffer(VkGlobalAllocator.getAllocator().getId(), bufferCreateInfo, allocationCreateInfo, pBuffer, pAllocation, allocationInfo);
        memory = allocationInfo.deviceMemory();

        handle = pBuffer.get();

    }

    private int toVkUsageType(Usage usage){
        switch(usage){
            case VertexBuffer -> {
                return VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
            }
            case IndexBuffer -> {
                return VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
            }
            case ShaderStorageBuffer -> {
                return VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
            }
            case UniformBuffer -> {
                return VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
            }
            case ImageBackingBuffer -> {
                return VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
            }
        }

        return 0;
    }

    private int toVkMemoryUsageType(Type type) {
        switch (type) {
            case GPULocal -> {
                return VMA_MEMORY_USAGE_GPU_ONLY;
            }
            case CPUGPUShared -> {
                return VMA_MEMORY_USAGE_CPU_TO_GPU;
            }
        }

        return 0;
    }


    public ByteBuffer map(){
        super.map();
        mappedMemory = MemoryUtil.memAllocPointer(1);
        vmaMapMemory(VkGlobalAllocator.getAllocator().getId(), pAllocation.get(0), mappedMemory);
        return mappedMemory.getByteBuffer(getSizeBytes());
    }



    public void unmap(){
        super.unmap();

        vmaUnmapMemory(VkGlobalAllocator.getAllocator().getId(), pAllocation.get(0));
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
        vkDeviceWaitIdle(VkContextManager.getCurrentDevice());
        vmaDestroyBuffer(VkGlobalAllocator.getAllocator().getId(), handle, pAllocation.get(0));



        MemoryUtil.memFree(pBuffer);
        MemoryUtil.memFree(pAllocation);

        allocationInfo.free();
        allocationCreateInfo.free();
    }

}
