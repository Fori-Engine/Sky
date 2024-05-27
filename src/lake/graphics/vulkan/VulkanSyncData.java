package lake.graphics.vulkan;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackGet;

/**
 * Wraps the needed sync objects for an in flight frame
 *
 * This frame's sync objects must be deleted manually
 * */
public class VulkanSyncData {

    public long imageAcquiredFence;
    public long submissionFence;


    public VulkanSyncData(long imageAcquiredFence, long submissionFence) {
        this.imageAcquiredFence = imageAcquiredFence;
        this.submissionFence = submissionFence;
    }





}