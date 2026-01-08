package engine.graphics.vulkan;

import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import java.nio.IntBuffer;

class VulkanSwapchainSupportInfo {

    public VkSurfaceCapabilitiesKHR capabilities;
    public VkSurfaceFormatKHR.Buffer formats;
    public IntBuffer presentModes;

}