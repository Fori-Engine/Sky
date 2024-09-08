package lake.graphics.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkCommandBuffer;

public class VkFrame {
    public long imageAcquiredSemaphore;
    public long renderFinishedSemaphore;
    public long inFlightFence;
    public VkCommandBuffer renderCommandBuffer;

    public VkFrame(long imageAcquiredSemaphore, long renderFinishedSemaphore, long inFlightFence) {
        this.imageAcquiredSemaphore = imageAcquiredSemaphore;
        this.renderFinishedSemaphore = renderFinishedSemaphore;
        this.inFlightFence = inFlightFence;
    }
}
