package lake.graphics.vulkan;

import lake.graphics.Disposable;
import lake.graphics.Disposer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VkImageView implements Disposable {
    private VkImage image;
    private VkImageViewCreateInfo imageViewCreateInfo;
    private LongBuffer pImageView;
    private long handle;


    public VkImageView(VkDevice device, VkImage image, int aspectMask) {
        Disposer.add("managedResources", this);
        this.image = image;

        imageViewCreateInfo = VkImageViewCreateInfo.create();
        pImageView = MemoryUtil.memAllocLong(1);


        imageViewCreateInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
        imageViewCreateInfo.image(image.getHandle());
        imageViewCreateInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
        imageViewCreateInfo.format(image.getFormat());

        imageViewCreateInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY);
        imageViewCreateInfo.components().g(VK_COMPONENT_SWIZZLE_IDENTITY);
        imageViewCreateInfo.components().b(VK_COMPONENT_SWIZZLE_IDENTITY);
        imageViewCreateInfo.components().a(VK_COMPONENT_SWIZZLE_IDENTITY);

        imageViewCreateInfo.subresourceRange().aspectMask(aspectMask);
        imageViewCreateInfo.subresourceRange().baseMipLevel(0);
        imageViewCreateInfo.subresourceRange().levelCount(1);
        imageViewCreateInfo.subresourceRange().baseArrayLayer(0);
        imageViewCreateInfo.subresourceRange().layerCount(1);

        if (vkCreateImageView(device, imageViewCreateInfo, null, pImageView) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create image views");
        }

        handle = pImageView.get(0);
    }

    public long getHandle() {
        return handle;
    }

    @Override
    public void dispose() {
        imageViewCreateInfo.free();
        MemoryUtil.memFree(pImageView);
    }
}
