package fori.graphics.vulkan;

public class VulkanQueueFamilies {
    public Integer graphicsFamily;
    public Integer presentFamily;

    public VulkanQueueFamilies(int graphicsFamily, int presentFamily) {
        this.graphicsFamily = graphicsFamily;
        this.presentFamily = presentFamily;
    }
}
