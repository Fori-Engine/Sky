package lake.graphics.vulkan;

import java.util.List;
import java.util.Map;

public class LVKRenderSync {
    public List<LVKRenderFrame> inFlightFrames;
    public Map<Integer, LVKRenderFrame> imagesInFlight;
}
