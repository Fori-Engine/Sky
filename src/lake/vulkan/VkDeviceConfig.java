package lake.vulkan;

import org.lwjgl.vulkan.VkDevice;

public class VkDeviceConfig {
    public VkDevice device;
    public QueueFamilyIndices queueFamilyIndices;

    public VkDeviceConfig(VkDevice device, QueueFamilyIndices queueFamilyIndices) {
        this.device = device;
        this.queueFamilyIndices = queueFamilyIndices;
    }
}
