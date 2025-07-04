package fori.graphics.vulkan;

import fori.graphics.Disposable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocationInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkImageCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.util.vma.Vma.vmaCreateImage;
import static org.lwjgl.util.vma.Vma.vmaDestroyImage;
import static org.lwjgl.vulkan.VK13.*;


public class VulkanImage extends Disposable {

    private long handle;
    private LongBuffer pImage;

    private VkImageCreateInfo imageCreateInfo;
    private VkExtent3D extent;
    private long memory;
    private PointerBuffer pAllocation;
    private VmaAllocationInfo allocationInfo;
    private VmaAllocationCreateInfo allocationCreateInfo;
    private VkDevice device;
    private int format;
    private VulkanAllocator allocator;

    public VulkanImage(Disposable parent, VkDevice device, long imageHandle, int imageFormat) {
        super(parent);
        this.device = device;
        this.handle = imageHandle;
        this.format = imageFormat;
    }

    public VulkanImage(Disposable parent, VulkanAllocator allocator, VkDevice device, int width, int height, int format, int usage, int tiling){
        super(parent);

        this.device = device;
        this.format = format;
        this.allocator = allocator;

        try(MemoryStack stack = MemoryStack.stackPush()) {


            extent = VkExtent3D.calloc(stack).set(width, height, 1);

            imageCreateInfo = VkImageCreateInfo.calloc(stack);
            imageCreateInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageCreateInfo.imageType(VK_IMAGE_TYPE_2D);
            imageCreateInfo.extent(extent);
            imageCreateInfo.mipLevels(1);
            imageCreateInfo.arrayLayers(1);
            imageCreateInfo.format(format);
            imageCreateInfo.tiling(tiling);
            imageCreateInfo.usage(usage);
            imageCreateInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageCreateInfo.samples(VK_SAMPLE_COUNT_1_BIT);
            imageCreateInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);


            allocationCreateInfo = VmaAllocationCreateInfo.calloc(stack);


            pImage = stack.callocLong(1);
            pAllocation = stack.callocPointer(1);


            allocationInfo = VmaAllocationInfo.calloc(stack);
            vmaCreateImage(allocator.getId(), imageCreateInfo, allocationCreateInfo, pImage, pAllocation, allocationInfo);

            memory = allocationInfo.deviceMemory();

            handle = pImage.get(0);
        }
    }

    public int getFormat() {
        return format;
    }

    public long getHandle() {
        return handle;
    }

    @Override
    public void dispose() {
        vkDeviceWaitIdle(VulkanDeviceManager.getCurrentDevice());
        if(pAllocation != null)
            vmaDestroyImage(VulkanAllocator.getAllocator().getId(), handle, pAllocation.get(0));
    }

}
