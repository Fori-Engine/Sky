package fori.graphics.vulkan;

import fori.graphics.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanComputeCommandList extends ComputeCommandList {

    private VkQueue computeQueue;
    private long commandPool;
    private VkCommandBuffer[] commandBuffers;
    private VkDevice device;
    protected VulkanFence[] submissionFences;

    public VulkanComputeCommandList(Disposable parent, int framesInFlight) {
        super(parent, framesInFlight);

        computeQueue = VulkanRuntime.getGraphicsQueue();
        device = VulkanRuntime.getCurrentDevice();
        commandPool = VulkanUtil.createCommandPool(device, VulkanRuntime.getGraphicsFamilyIndex());


        submissionFences = new VulkanFence[framesInFlight];
        finishedSemaphores = new VulkanSemaphore[framesInFlight];
        commandBuffers = new VkCommandBuffer[framesInFlight];

        for (int i = 0; i < framesInFlight; i++) {
            finishedSemaphores[i] = new VulkanSemaphore(this, device);
            submissionFences[i] = new VulkanFence(this, device, VK_FENCE_CREATE_SIGNALED_BIT);


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
    public void copyTextures(Texture src, Texture dst) {

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
    public void startRecording(Semaphore[] waitSemaphores, RenderTarget renderTarget, int frameIndex) {
        super.startRecording(waitSemaphores, renderTarget, frameIndex);
        vkWaitForFences(device, submissionFences[frameIndex].getHandle(), true, VulkanUtil.UINT64_MAX);


        vkResetFences(device, submissionFences[frameIndex].getHandle());
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
    public void run() {
        try(MemoryStack stack = stackPush()) {

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.waitSemaphoreCount(1);

            submitInfo.pWaitSemaphores(stack.longs(((VulkanSemaphore) waitSemaphores[frameIndex]).getHandle()));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(commandBuffers[frameIndex]));
            submitInfo.pSignalSemaphores(stack.longs(((VulkanSemaphore) finishedSemaphores[frameIndex]).getHandle()));

            vkQueueSubmit(computeQueue, submitInfo, ((VulkanFence) submissionFences[frameIndex]).getHandle());
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
