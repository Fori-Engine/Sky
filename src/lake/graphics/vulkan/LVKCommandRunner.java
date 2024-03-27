package lake.graphics.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;

public class LVKCommandRunner {

    public static long commandPool;
    private static VkQueue graphicsQueue;

    public static void setup(VkDeviceWithIndices deviceWithIndices, VkQueue graphicsQueue){
        commandPool = FastVK.createCommandPool(deviceWithIndices);
        LVKCommandRunner.graphicsQueue = graphicsQueue;
    }



    public static void run(VkDevice device, MemoryStack stack, Command r){
        PointerBuffer pCommandBuffer = stack.mallocPointer(1);

        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
        allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
        allocInfo.commandPool(commandPool);
        allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
        allocInfo.commandBufferCount(1);

        if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate command buffers");
        }

        VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(), device);

        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
        beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

        if(vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
            throw new RuntimeException("Failed to begin recording command buffer");
        }

        r.run(commandBuffer);

        if(vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer");
        }

        VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
        {
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(stack.pointers(commandBuffer));
        }



        if(vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE) != VK_SUCCESS) {
            throw new RuntimeException("Failed to submit command buffer");
        }

        vkQueueWaitIdle(graphicsQueue);

    }

    public static void cleanup(VkDevice device){
        vkDestroyCommandPool(device, commandPool, null);
    }

    public interface Command {
        void run(VkCommandBuffer commandBuffer);
    }

}
