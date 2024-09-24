package lake.graphics.vulkan;

import lake.graphics.Disposable;
import lake.graphics.Disposer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public class VkImageView implements Disposable {
    private VkImage image;
    private VkImageViewCreateInfo imageViewCreateInfo;

    public VkImageView(VkDevice device, VkImage image) {
        Disposer.add("managedResources", this);
        this.image = image;

        // = VkImageViewCreateInfo.calloc(stack);

        /*
        createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
        createInfo.image(image.getHandle());
        createInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
        createInfo.format(image.getFormat());

        createInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY);
        createInfo.components().g(VK_COMPONENT_SWIZZLE_IDENTITY);
        createInfo.components().b(VK_COMPONENT_SWIZZLE_IDENTITY);
        createInfo.components().a(VK_COMPONENT_SWIZZLE_IDENTITY);

        createInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        createInfo.subresourceRange().baseMipLevel(0);
        createInfo.subresourceRange().levelCount(1);
        createInfo.subresourceRange().baseArrayLayer(0);
        createInfo.subresourceRange().layerCount(1);

        if (vkCreateImageView(device, createInfo, null, pImageView) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create image views");
        }

         */


    }

    @Override
    public void dispose() {

    }
}
