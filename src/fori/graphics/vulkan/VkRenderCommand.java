package fori.graphics.vulkan;

import fori.Logger;
import fori.graphics.Buffer;
import fori.graphics.RenderCommand;
import fori.graphics.Texture;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static fori.graphics.vulkan.VkRenderer.UINT64_MAX;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class VkRenderCommand extends RenderCommand {
    public VkPipeline pipeline;
    public VkCommandBuffer commandBuffer;
    public long fence;
    public VkQueue graphicsQueue;
    public VkDevice device;
    public Texture[] textures;

    public VkRenderCommand(int framesInFlight, long commandPool, VkQueue graphicsQueue, VkDevice device, Texture[] textures) {
        super(framesInFlight);

        this.textures = textures;
        this.graphicsQueue = graphicsQueue;
        this.device = device;
        transformsBuffer = new VkBuffer[framesInFlight];
        cameraBuffer = new VkBuffer[framesInFlight];

        try(MemoryStack stack = stackPush()) {

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffers = stack.mallocPointer(1);

            if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                throw new RuntimeException(Logger.error(VkRenderCommand.class, "Failed to create per-RenderCommand command buffer"));
            }

            commandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), device);

            LongBuffer pFence = stack.mallocLong(1);

            VkFenceCreateInfo transferFenceCreateInfo = VkFenceCreateInfo.calloc(stack);
            transferFenceCreateInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            vkCreateFence(device, transferFenceCreateInfo, null, pFence);

            fence = pFence.get(0);
        }
    }

    @Override
    public Buffer getDefaultVertexBuffer() {
        return stagingVertexBuffer;
    }

    @Override
    public Buffer getDefaultIndexBuffer() {
        return stagingIndexBuffer;
    }

    @Override
    public void update() {

        try(MemoryStack stack = stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                throw new RuntimeException(Logger.error(VkRenderCommand.class, "Failed to start recording per-RenderCommand command buffer"));
            }



            for(Texture texture : textures){

                VkTexture vkTexture = (VkTexture) texture;

                vkTexture.transitionImageLayout(
                        commandBuffer,
                        VK_IMAGE_LAYOUT_UNDEFINED,
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL
                );

                VkBufferImageCopy.Buffer imageCopies = VkBufferImageCopy.calloc(1, stack);
                imageCopies.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                imageCopies.imageSubresource().layerCount(1);
                imageCopies.imageExtent().set(texture.getWidth(), texture.getHeight(), 1);


                vkCmdCopyBufferToImage(commandBuffer, ((VkBuffer) vkTexture.imageData).getHandle(), vkTexture.image.getHandle(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, imageCopies);

                vkTexture.transitionImageLayout(
                        commandBuffer,
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                );


            }



            VkBufferCopy.Buffer vertexBufferCopy = VkBufferCopy.calloc(1, stack);
            vertexBufferCopy.size(stagingVertexBuffer.getSizeBytes());
            vkCmdCopyBuffer(commandBuffer, ((VkBuffer) stagingVertexBuffer).getHandle(), ((VkBuffer) vertexBuffer).getHandle(), vertexBufferCopy);


            VkBufferCopy.Buffer indexBufferCopy = VkBufferCopy.calloc(1, stack);
            indexBufferCopy.size(stagingIndexBuffer.getSizeBytes());
            vkCmdCopyBuffer(commandBuffer, ((VkBuffer) stagingIndexBuffer).getHandle(), ((VkBuffer) indexBuffer).getHandle(), indexBufferCopy);



            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                throw new RuntimeException(Logger.error(VkRenderCommand.class, "Failed to finish recording per-RenderCommand command buffer"));
            }

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(stack.pointers(commandBuffer));

            if(vkQueueSubmit(graphicsQueue, submitInfo, fence) != VK_SUCCESS) {
                throw new RuntimeException(Logger.error(VkRenderCommand.class, "Failed to submit per-RenderCommand command buffer"));
            }

            vkWaitForFences(device, fence, true, UINT64_MAX);


        }
    }
}
