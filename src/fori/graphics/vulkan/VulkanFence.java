package fori.graphics.vulkan;

import fori.graphics.Disposable;
import fori.graphics.Fence;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanFence extends Fence {

    private VkDevice device;
    private long handle;


    public VulkanFence(Disposable parent, VkDevice device, int flags) {
        super(parent);
        this.device = device;

        try(MemoryStack stack = MemoryStack.stackPush()) {

            VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(stack);
            fenceCreateInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceCreateInfo.flags(flags);
            LongBuffer pFence = stack.mallocLong(1);

            vkCreateFence(device, fenceCreateInfo, null, pFence);
            handle = pFence.get(0);
        }
    }

    public long getHandle() {
        return handle;
    }


    @Override
    public void dispose() {
        vkDeviceWaitIdle(device);
        vkDestroyFence(device, handle, null);
    }
}
