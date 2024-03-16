package lake.vulkan;

public class QueueFamilyIndices {
    public Integer graphicsFamily;
    public boolean isComplete() {
        return graphicsFamily != null;
    }

}