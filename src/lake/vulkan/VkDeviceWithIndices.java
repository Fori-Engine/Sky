package lake.vulkan;

import org.lwjgl.vulkan.VkDevice;

public class VkDeviceWithIndices {
    public VkDevice device;
    public QueueFamilyIndices queueFamilyIndices;

    public VkDeviceWithIndices(VkDevice device, QueueFamilyIndices queueFamilyIndices) {
        this.device = device;
        this.queueFamilyIndices = queueFamilyIndices;
    }
}
