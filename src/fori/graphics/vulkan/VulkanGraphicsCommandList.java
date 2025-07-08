package fori.graphics.vulkan;

import fori.graphics.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class VulkanGraphicsCommandList extends GraphicsCommandList {

    private VkQueue submitQueue;
    private long commandPool;
    private VkCommandBuffer[] commandBuffers;
    private VkDevice device;
    private VkRenderingInfoKHR renderingInfoKHR;
    private VkRenderingAttachmentInfo.Buffer colorAttachment;
    private VkRenderingAttachmentInfoKHR depthAttachment;
    protected VulkanFence[] submissionFences;


    public VulkanGraphicsCommandList(Disposable parent, int framesInFlight) {
        super(parent, framesInFlight);

        submitQueue = VulkanDeviceManager.getGraphicsQueue();
        device = VulkanDeviceManager.getCurrentDevice();
        commandPool = VulkanUtil.createCommandPool(device, VulkanDeviceManager.getGraphicsFamilyIndex());


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
    public void setDrawBuffers(Buffer vertexBuffer, Buffer indexBuffer) {
        try(MemoryStack stack = stackPush()) {
            VulkanBuffer vulkanVertexBuffer = (VulkanBuffer) vertexBuffer;
            VulkanBuffer vulkanIndexBuffer = (VulkanBuffer) indexBuffer;

            LongBuffer vertexBuffers = stack.longs(vulkanVertexBuffer.getHandle());
            LongBuffer offsets = stack.longs(0);

            vkCmdBindVertexBuffers(commandBuffers[frameIndex], 0, vertexBuffers, offsets);
            vkCmdBindIndexBuffer(commandBuffers[frameIndex], vulkanIndexBuffer.getHandle(), 0, VK_INDEX_TYPE_UINT32);


        }
    }

    @Override
    public void setShaderProgram(ShaderProgram shaderProgram) {
        try(MemoryStack stack = stackPush()) {
            VulkanShaderProgram vulkanShaderProgram = (VulkanShaderProgram) shaderProgram;
            VulkanPipeline pipeline = ((VulkanShaderProgram) shaderProgram).getPipeline();
            vkCmdBindPipeline(commandBuffers[frameIndex], VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle());
            vkCmdBindDescriptorSets(commandBuffers[frameIndex], VK_PIPELINE_BIND_POINT_GRAPHICS,
                    pipeline.getLayout(), 0, stack.longs(vulkanShaderProgram.getDescriptorSets(frameIndex)), null);

        }
    }

    @Override
    public void drawIndexed(int indexCount) {
        vkCmdDrawIndexed(
                commandBuffers[frameIndex],
                indexCount,
                1,
                0,
                0,
                0
        );
    }


    @Override
    public void startRecording(Semaphore[] waitSemaphores, RenderTarget renderTarget, int frameIndex) {
        super.startRecording(waitSemaphores, renderTarget, frameIndex);
        vkWaitForFences(device, submissionFences[frameIndex].getHandle(), true, VulkanUtil.UINT64_MAX);


        vkResetFences(device, submissionFences[frameIndex].getHandle());
        vkResetCommandBuffer(commandBuffers[frameIndex], 0);

        try(MemoryStack stack = stackPush()) {

            VkClearValue colorClearValue = VkClearValue.calloc(stack);
            colorClearValue.color().float32(stack.floats(0.5f, 0.5f, 0.5f, 1.0f));

            VkClearValue depthClearValue = VkClearValue.calloc(stack);
            depthClearValue.depthStencil().set(1.0f, 0);
            VulkanTexture texture = (VulkanTexture) renderTarget.getTexture(frameIndex);



            colorAttachment = VkRenderingAttachmentInfoKHR.calloc(1, stack);
            {
                /*TODO(Shayan) BAD BAD BAD! Making assumptions about which image will be the depth image
                   for a given rendertarget!*/


                colorAttachment.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
                colorAttachment.imageView(texture.getImageView().getHandle());
                colorAttachment.imageLayout(VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR);
                colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
                colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
                colorAttachment.clearValue(colorClearValue);
            }
            depthAttachment = VkRenderingAttachmentInfoKHR.calloc(stack);
            {

                /*TODO(Shayan) BAD BAD BAD! Making assumptions about which image will be the depth image
                   for a given rendertarget!*/
                VulkanTexture depthImage = ((VulkanTexture) renderTarget.getTexture(framesInFlight));
                VulkanImageView depthImageView = depthImage.getImageView();

                depthAttachment.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
                depthAttachment.imageView(depthImageView.getHandle());
                depthAttachment.imageLayout(VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR);
                depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
                depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
                depthAttachment.clearValue(depthClearValue);
            }

            renderingInfoKHR = VkRenderingInfoKHR.calloc(stack);
            renderingInfoKHR.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_INFO_KHR);
            VkRect2D renderArea = VkRect2D.calloc(stack);
            renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
            renderArea.extent(VkExtent2D.calloc(stack).set(texture.getWidth(), texture.getHeight()));
            renderingInfoKHR.renderArea(renderArea);
            renderingInfoKHR.layerCount(1);
            renderingInfoKHR.pColorAttachments(colorAttachment);
            renderingInfoKHR.pDepthAttachment(depthAttachment);


            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            if (vkBeginCommandBuffer(commandBuffers[frameIndex], beginInfo) != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer");
            }

            VkImageMemoryBarrier.Buffer startBarrier = VkImageMemoryBarrier.calloc(1, stack);
            {
                startBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                startBarrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                startBarrier.newLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                startBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                startBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                startBarrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                startBarrier.image(((VulkanTexture) renderTarget.getTexture(frameIndex)).getImage().getHandle());
                startBarrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                startBarrier.subresourceRange().baseMipLevel(0);
                startBarrier.subresourceRange().levelCount(1);
                startBarrier.subresourceRange().baseArrayLayer(0);
                startBarrier.subresourceRange().layerCount(1);
            }


            vkCmdPipelineBarrier(
                    commandBuffers[frameIndex],
                    VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                    0,
                    null,
                    null,
                    startBarrier
            );

            KHRDynamicRendering.vkCmdBeginRenderingKHR(commandBuffers[frameIndex], renderingInfoKHR);
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(0.0f);
            viewport.y(0.0f);
            viewport.width(texture.getWidth());
            viewport.height(texture.getHeight());


            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);


            vkCmdSetViewport(commandBuffers[frameIndex], 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            {
                scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
                scissor.extent(VkExtent2D.calloc(stack).set(texture.getWidth(), texture.getHeight()));
            }

            vkCmdSetScissor(commandBuffers[frameIndex], 0, scissor);

        }

    }

    @Override
    public void endRecording() {

        try(MemoryStack stack = stackPush()) {

            KHRDynamicRendering.vkCmdEndRenderingKHR(commandBuffers[frameIndex]);


            VkImageMemoryBarrier.Buffer endBarrier = VkImageMemoryBarrier.calloc(1, stack);
            {
                endBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                endBarrier.oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                endBarrier.newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
                endBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                endBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                endBarrier.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                endBarrier.image(((VulkanTexture) renderTarget.getTexture(frameIndex)).getImage().getHandle());
                endBarrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                endBarrier.subresourceRange().baseMipLevel(0);
                endBarrier.subresourceRange().levelCount(1);
                endBarrier.subresourceRange().baseArrayLayer(0);
                endBarrier.subresourceRange().layerCount(1);
            }

            vkCmdPipelineBarrier(
                    commandBuffers[frameIndex],
                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                    VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    0,
                    null,
                    null,
                    endBarrier
            );

            if (vkEndCommandBuffer(commandBuffers[frameIndex]) != VK_SUCCESS) {
                throw new RuntimeException("Failed to record command buffer");
            }
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

            vkQueueSubmit(submitQueue, submitInfo, ((VulkanFence) submissionFences[frameIndex]).getHandle());
        }


    }


    @Override
    public void dispose() {
        vkDeviceWaitIdle(VulkanDeviceManager.getCurrentDevice());
        vkDestroyCommandPool(device, commandPool, null);
    }
}
