package fori.graphics.vulkan;

import fori.graphics.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.util.Optional;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanComputeCommandList extends ComputeCommandList {

    private VkQueue computeQueue;
    private long commandPool;
    private VkCommandBuffer[] commandBuffers;
    private VkDevice device;

    public VulkanComputeCommandList(Disposable parent, int framesInFlight) {
        super(parent, framesInFlight);

        computeQueue = VulkanRuntime.getGraphicsQueue();
        device = VulkanRuntime.getCurrentDevice();
        commandPool = VulkanUtil.createCommandPool(device, VulkanRuntime.getGraphicsFamilyIndex());


        finishedSemaphores = new VulkanSemaphore[framesInFlight];
        commandBuffers = new VkCommandBuffer[framesInFlight];

        for (int i = 0; i < framesInFlight; i++) {
            finishedSemaphores[i] = new VulkanSemaphore(this, device);


            try(MemoryStack stack = stackPush()) {
                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(commandPool);
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandBufferCount(framesInFlight);

                PointerBuffer pCommandBuffers = stack.mallocPointer(framesInFlight);

                if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate command buffers");
                }

                commandBuffers[i] = new VkCommandBuffer(pCommandBuffers.get(i), device);
            }
        }
    }



    @Override
    public void setShaderProgram(ShaderProgram shaderProgram) {
        try(MemoryStack stack = stackPush()) {
            VulkanShaderProgram vulkanShaderProgram = (VulkanShaderProgram) shaderProgram;
            VulkanPipeline pipeline = ((VulkanShaderProgram) shaderProgram).getPipeline();
            vkCmdBindPipeline(commandBuffers[frameIndex], VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.getHandle());
            vkCmdBindDescriptorSets(commandBuffers[frameIndex], VK_PIPELINE_BIND_POINT_COMPUTE,
                    pipeline.getLayout(), 0, stack.longs(vulkanShaderProgram.getDescriptorSets(frameIndex)), null);
        }
    }

    @Override
    public void startRecording(Semaphore[] waitSemaphores, int frameIndex) {
        super.startRecording(waitSemaphores, frameIndex);

        vkResetCommandBuffer(commandBuffers[frameIndex], 0);

        try(MemoryStack stack = stackPush()) {


            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            if (vkBeginCommandBuffer(commandBuffers[frameIndex], beginInfo) != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer");
            }
        }
    }

    @Override
    public void dispatch(int groupCountX, int groupCountY, int groupCountZ) {
        vkCmdDispatch(commandBuffers[frameIndex], groupCountX, groupCountY, groupCountZ);
    }


    @Override
    public void endRecording() {
        if (vkEndCommandBuffer(commandBuffers[frameIndex]) != VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer");
        }
    }

    @Override
    public void run(Optional<Fence[]> submissionFences) {
        try(MemoryStack stack = stackPush()) {

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.waitSemaphoreCount(1);

            submitInfo.pWaitSemaphores(stack.longs(((VulkanSemaphore) waitSemaphores[frameIndex]).getHandle()));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(commandBuffers[frameIndex]));
            submitInfo.pSignalSemaphores(stack.longs(((VulkanSemaphore) finishedSemaphores[frameIndex]).getHandle()));

            if(submissionFences.isPresent())
                vkQueueSubmit(computeQueue, submitInfo, ((VulkanFence[]) submissionFences.get())[frameIndex].getHandle());
            else vkQueueSubmit(computeQueue, submitInfo, VK_NULL_HANDLE);
        }
    }

    @Override
    public void waitForFinish() {
        vkQueueWaitIdle(computeQueue);
    }

    @Override
    public void dispose() {
        vkDeviceWaitIdle(VulkanRuntime.getCurrentDevice());
        vkDestroyCommandPool(device, commandPool, null);
    }
}
