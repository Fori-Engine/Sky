package fori.graphics.vulkan;

import fori.graphics.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.Optional;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanComputePass extends ComputePass {

    private VkQueue computeQueue;
    private VkCommandBuffer[] commandBuffers;
    private VkDevice device;
    private VulkanCommandPool commandPool;

    public VulkanComputePass(Disposable parent, String name, int framesInFlight) {
        super(parent, name, framesInFlight);

        computeQueue = VulkanRuntime.getGraphicsQueue();
        device = VulkanRuntime.getCurrentDevice();

        //TODO(Shayan) This should use the compute family index!
        commandPool = new VulkanCommandPool(this, device, VulkanRuntime.getGraphicsFamilyIndex());



        finishedSemaphores = new VulkanSemaphore[framesInFlight];
        commandBuffers = new VkCommandBuffer[framesInFlight];

        for (int i = 0; i < framesInFlight; i++) {
            finishedSemaphores[i] = new VulkanSemaphore(this, device);


            try(MemoryStack stack = stackPush()) {
                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(commandPool.getHandle());
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
    public void setWritable(RenderTarget renderTarget) {

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
    public void startRecording(int frameIndex) {
        super.startRecording(frameIndex);

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
    public void submit(Optional<Fence[]> submissionFences) {
        try(MemoryStack stack = stackPush()) {

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.waitSemaphoreCount(1);

            submitInfo.pWaitSemaphores(stack.longs(((VulkanSemaphore) waitSemaphores[frameIndex]).getHandle()));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT));
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
    public void resolveResourceDependencies() {
        barrierCallback.run(commandBuffers[frameIndex]);
    }


    @Override
    public void dispose() {}
}
