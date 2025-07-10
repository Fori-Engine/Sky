package fori.graphics.vulkan;

import fori.graphics.TextureFormatType;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class VulkanUtil {
    public static final int UINT64_MAX = 0xFFFFFFFF;

    public static long createCommandPool(VkDevice device, int queueFamily) {

        long commandPool = 0;

        try(MemoryStack stack = stackPush()) {
            VkCommandPoolCreateInfo commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(stack);
            commandPoolCreateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            commandPoolCreateInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
            commandPoolCreateInfo.queueFamilyIndex(queueFamily);

            LongBuffer pCommandPool = stack.mallocLong(1);

            if(vkCreateCommandPool(device, commandPoolCreateInfo, null, pCommandPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool");
            }
            commandPool = pCommandPool.get(0);
        }

        return commandPool;
    }

    public static int toVkTextureFormatEnum(TextureFormatType textureFormatType) {
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

}
