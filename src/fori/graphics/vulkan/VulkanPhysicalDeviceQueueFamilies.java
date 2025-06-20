package fori.graphics.vulkan;

public class VulkanPhysicalDeviceQueueFamilies {
    public Integer graphicsFamily;
    public Integer presentFamily;

    public VulkanPhysicalDeviceQueueFamilies(int graphicsFamily, int presentFamily) {
        this.graphicsFamily = graphicsFamily;
        this.presentFamily = presentFamily;
    }
}
