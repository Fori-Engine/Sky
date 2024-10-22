package fori.graphics.vulkan;

import fori.graphics.Disposable;
import fori.graphics.Ref;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VkImageView implements Disposable {
    private VkImage image;
    private VkImageViewCreateInfo imageViewCreateInfo;
    private long handle;
    private Ref ref;


    public VkImageView(Ref parent, VkDevice device, VkImage image, int aspectMask) {
        ref = parent.add(this);
        this.image = image;

        imageViewCreateInfo = VkImageViewCreateInfo.create();
        LongBuffer pImageView = MemoryUtil.memAllocLong(1);


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
        MemoryUtil.memFree(pImageView);
    }

    public long getHandle() {
        return handle;
    }

    @Override
    public void dispose() {
        vkDeviceWaitIdle(VkContextManager.getCurrentDevice());
        vkDestroyImageView(VkContextManager.getCurrentDevice(), handle, null);
        imageViewCreateInfo.free();
    }

    @Override
    public Ref getRef() {
        return ref;
    }
}
