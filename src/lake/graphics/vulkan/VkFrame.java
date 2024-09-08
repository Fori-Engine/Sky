package lake.graphics.vulkan;

public class VkFrame {
    public long imageAcquiredSemaphore;
    public long renderFinishedSemaphore;
    public long inFlightFence;

    public VkFrame(long imageAcquiredSemaphore, long renderFinishedSemaphore, long inFlightFence) {
        this.imageAcquiredSemaphore = imageAcquiredSemaphore;
        this.renderFinishedSemaphore = renderFinishedSemaphore;
        this.inFlightFence = inFlightFence;
    }
}
