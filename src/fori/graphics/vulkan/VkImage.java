package fori.graphics.vulkan;

import fori.graphics.Disposable;
import fori.graphics.Ref;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocationInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkImageCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.util.vma.Vma.vmaCreateImage;
import static org.lwjgl.util.vma.Vma.vmaDestroyImage;
import static org.lwjgl.vulkan.VK13.*;


public class VkImage implements Disposable {

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
    private VkGlobalAllocator allocator;
    private Ref ref;

    public VkImage(Ref parent, VkGlobalAllocator allocator, VkDevice device, int width, int height, int format, int usage, int tiling){
        super();
        ref = parent.add(this);



        this.device = device;
        this.format = format;
        this.allocator = allocator;


        extent = VkExtent3D.create().set(width, height, 1);

        imageCreateInfo = VkImageCreateInfo.create();
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



        allocationCreateInfo = VmaAllocationCreateInfo.create();



        pImage = MemoryUtil.memAllocLong(1);
        pAllocation = MemoryUtil.memAllocPointer(1);


        allocationInfo = VmaAllocationInfo.create();
        vmaCreateImage(allocator.getId(), imageCreateInfo, allocationCreateInfo, pImage, pAllocation, allocationInfo);

        memory = allocationInfo.deviceMemory();

        handle = pImage.get(0);
    }

    public int getFormat() {
        return format;
    }

    public long getHandle() {
        return handle;
    }

    @Override
    public void dispose() {
        vkDeviceWaitIdle(VkContextManager.getCurrentDevice());
        vmaDestroyImage(VkGlobalAllocator.getAllocator().getId(), handle, pAllocation.get(0));

        //extent.free();
        //imageCreateInfo.free();
        MemoryUtil.memFree(pImage);
        MemoryUtil.memFree(pAllocation);

        //allocationInfo.free();
        //allocationCreateInfo.free();


    }

    @Override
    public Ref getRef() {
        return ref;
    }


}
