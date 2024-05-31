package lake.graphics.vulkan;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackGet;

/**
 * Wraps the needed sync objects for an in flight frame
 *
 * This frame's sync objects must be deleted manually
 * */
public class VulkanSyncData {

    public long imageAcquiredSemaphore;
    public long renderFinishedSemaphore;
    public long submissionFence;


    public VulkanSyncData(long imageAcquiredSemaphore, long renderFinishedSemaphore, long submissionFence) {
        this.imageAcquiredSemaphore = imageAcquiredSemaphore;
        this.renderFinishedSemaphore = renderFinishedSemaphore;
        this.submissionFence = submissionFence;
    }
}