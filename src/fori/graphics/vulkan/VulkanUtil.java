package fori.graphics.vulkan;

import fori.graphics.TextureFormatType;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class VulkanUtil {
    public static final int UINT64_MAX = 0xFFFFFFFF;


    public static int toVkImageFormatEnum(TextureFormatType textureFormatType) {
        switch (textureFormatType) {
            case ColorR8G8B8A8StandardRGB -> {
                return VK_FORMAT_R8G8B8A8_SRGB;
            }
            case Depth32Float -> {
                return VK_FORMAT_D32_SFLOAT;
            }
        }
        return -1;
    }

    public static void transitionImageLayout(VulkanImage image,
                                        VkCommandBuffer commandBuffer,
                                        int newLayout,
                                        int srcAccessMask,
                                        int dstAccessMask,
                                        int aspectMask,
                                        int srcStageMask,
                                        int dstStageMask) {
        try(MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer imageBarrier = VkImageMemoryBarrier.calloc(1, stack);
            {
                imageBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                imageBarrier.oldLayout(image.getCurrentLayout());
                imageBarrier.newLayout(newLayout);
                imageBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                imageBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                imageBarrier.srcAccessMask(srcAccessMask);
                imageBarrier.dstAccessMask(dstAccessMask);
                imageBarrier.image(image.getHandle());
                imageBarrier.subresourceRange().aspectMask(aspectMask);
                imageBarrier.subresourceRange().baseMipLevel(0);
                imageBarrier.subresourceRange().levelCount(1);
                imageBarrier.subresourceRange().baseArrayLayer(0);
                imageBarrier.subresourceRange().layerCount(1);
            }



            vkCmdPipelineBarrier(
                    commandBuffer,
                    srcStageMask,
                    dstStageMask,
                    0,
                    null,
                    null,
                    imageBarrier
            );

            System.out.println("Layout change to " + newLayout + " finished!");

            image.setCurrentLayout(newLayout);
        }
    }

}
