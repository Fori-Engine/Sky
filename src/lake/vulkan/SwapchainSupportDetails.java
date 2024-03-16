package lake.vulkan;

import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import java.nio.IntBuffer;

class SwapChainSupportDetails {

    public VkSurfaceCapabilitiesKHR capabilities;
    public VkSurfaceFormatKHR.Buffer formats;
    public IntBuffer presentModes;

}