package lake.graphics.vulkan;

import java.util.List;
import java.util.Map;

public class VulkanRenderSyncInfo {
    public List<Frame> inFlightFrames;
    public Map<Integer, Frame> imagesInFlight;
}
