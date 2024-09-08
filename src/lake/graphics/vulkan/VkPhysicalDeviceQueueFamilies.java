package lake.graphics.vulkan;

public class VkPhysicalDeviceQueueFamilies {
    public Integer graphicsFamily;
    public Integer presentFamily;

    public VkPhysicalDeviceQueueFamilies(int graphicsFamily, int presentFamily) {
        this.graphicsFamily = graphicsFamily;
        this.presentFamily = presentFamily;
    }
}
