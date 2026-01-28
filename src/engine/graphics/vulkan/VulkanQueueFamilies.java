package engine.graphics.vulkan;

public class VulkanQueueFamilies {
    public int graphicsFamily;
    public int computeFamily;
    public int presentFamily;

    public VulkanQueueFamilies(int graphicsFamily, int computeFamily, int presentFamily) {
        this.graphicsFamily = graphicsFamily;
        this.computeFamily = computeFamily;
        this.presentFamily = presentFamily;
    }
}
