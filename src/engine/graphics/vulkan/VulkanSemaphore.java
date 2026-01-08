package engine.graphics.vulkan;

import engine.graphics.Disposable;
import engine.graphics.Semaphore;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkCreateSemaphore;

public class VulkanSemaphore extends Semaphore {

    private VkDevice device;
    private long handle;

    public VulkanSemaphore(Disposable parent, VkDevice device) {
        super(parent);
        this.device = device;

        try(MemoryStack stack = stackPush()) {

            LongBuffer pSemaphore = stack.mallocLong(1);

            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreCreateInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            vkCreateSemaphore(device, semaphoreCreateInfo, null, pSemaphore);
            handle = pSemaphore.get(0);

        }

    }

    public long getHandle() {
        return handle;
    }

    @Override
    public void dispose() {
        vkDeviceWaitIdle(device);
        vkDestroySemaphore(device, handle, null);
    }
}
