package fori.graphics.vulkan;

import org.lwjgl.vulkan.VkCommandBuffer;

public class VulkanFrame {
    public long imageAcquiredSemaphore;
    public long renderFinishedSemaphore;
    public long inFlightFence;
    public VkCommandBuffer renderCommandBuffer;

    public VulkanFrame(long imageAcquiredSemaphore, long renderFinishedSemaphore, long inFlightFence) {
        this.imageAcquiredSemaphore = imageAcquiredSemaphore;
        this.renderFinishedSemaphore = renderFinishedSemaphore;
        this.inFlightFence = inFlightFence;
    }
}
