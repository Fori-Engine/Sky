package fori.graphics.vulkan;

import fori.Logger;
import fori.asset.Asset;
import fori.asset.TextureData;
import fori.graphics.Buffer;
import fori.graphics.Disposable;
import fori.graphics.Texture;
import fori.graphics.TextureFormatType;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import static fori.graphics.Texture.Filter.Nearest;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanTexture extends Texture {

    private VulkanImage image;
    private VulkanImageView imageView;
    private VulkanSampler sampler;
    private Buffer imageData;
    private VkCommandBuffer commandBuffer;
    private VulkanFence fence;
    private long commandPool = 0;


    public VulkanTexture(Disposable parent, int width, int height, long imageHandle, int imageFormat, int aspectMask) {
        super(parent, width, height, null, toTextureFormatType(imageFormat), Nearest, Nearest);
        image = new VulkanImage(
                this,
                imageHandle,
                imageFormat
        );
        imageView = new VulkanImageView(image, VulkanRuntime.getCurrentDevice(), image, aspectMask);
        sampler = new VulkanSampler(this, minFilter, magFilter, false);
    }

    public VulkanTexture(Disposable parent, int width, int height, Asset<TextureData> textureData, Filter minFilter, Filter magFilter) {
        this(parent, width, height, textureData, minFilter, magFilter, VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, VK_IMAGE_TILING_OPTIMAL, VK_IMAGE_ASPECT_COLOR_BIT);
    }
    public VulkanTexture(Disposable parent, int width, int height, Asset<TextureData> textureData, Filter minFilter, Filter magFilter, int imageFormat, int usage, int tiling, int aspectMask) {
        super(parent, width, height, textureData, toTextureFormatType(imageFormat), minFilter, magFilter);
        formatType = toTextureFormatType(imageFormat);

        image = new VulkanImage(
                this,
                VulkanAllocator.getAllocator(),
                getWidth(),
                getHeight(),
                imageFormat,
                usage,
                tiling
        );

        imageView = new VulkanImageView(image, VulkanRuntime.getCurrentDevice(), image, aspectMask);
        sampler = new VulkanSampler(this, minFilter, magFilter, false);

        if(textureData != null) {


            imageData = Buffer.newBuffer(this, getWidth() * getHeight() * 4, Buffer.Usage.ImageBackingBuffer, Buffer.Type.CPUGPUShared, false);
            ByteBuffer bytes = imageData.map();
            bytes.put(getTextureData());
            imageData.unmap();




            try (MemoryStack stack = stackPush()) {


                VkCommandPoolCreateInfo commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(stack);
                commandPoolCreateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
                commandPoolCreateInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
                commandPoolCreateInfo.queueFamilyIndex(VulkanRuntime.getGraphicsFamilyIndex());

                LongBuffer pCommandPool = stack.mallocLong(1);

                if (vkCreateCommandPool(VulkanRuntime.getCurrentDevice(), commandPoolCreateInfo, null, pCommandPool) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create command pool");
                }
                commandPool = pCommandPool.get(0);

                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(commandPool);
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandBufferCount(1);

                PointerBuffer pCommandBuffers = stack.mallocPointer(1);

                if (vkAllocateCommandBuffers(VulkanRuntime.getCurrentDevice(), allocInfo, pCommandBuffers) != VK_SUCCESS) {
                    throw new RuntimeException(Logger.error(VulkanStaticMeshBatch.class, "Failed to create command buffer"));
                }

                commandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), VulkanRuntime.getCurrentDevice());

                fence = new VulkanFence(this, VulkanRuntime.getCurrentDevice(), 0);


                vkResetFences(VulkanRuntime.getCurrentDevice(), fence.getHandle());

                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
                beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

                if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                    throw new RuntimeException(Logger.error(VulkanStaticMeshBatch.class, "Failed to start recording command buffer"));
                }

                transitionImageLayout(
                        commandBuffer,
                        VK_IMAGE_LAYOUT_UNDEFINED,
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL
                );

                VkBufferImageCopy.Buffer imageCopies = VkBufferImageCopy.calloc(1, stack);
                imageCopies.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                imageCopies.imageSubresource().layerCount(1);
                imageCopies.imageExtent().set(width, height, 1);


                vkCmdCopyBufferToImage(commandBuffer, ((VulkanBuffer) imageData).getHandle(), image.getHandle(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, imageCopies);

                transitionImageLayout(
                        commandBuffer,
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                );

                if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                    throw new RuntimeException(Logger.error(VulkanTexture.class, "Failed to finish recording per-RenderCommand command buffer"));
                }

                VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
                submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
                submitInfo.pCommandBuffers(stack.pointers(commandBuffer));

                if (vkQueueSubmit(VulkanRuntime.getGraphicsQueue(), submitInfo, fence.getHandle()) != VK_SUCCESS) {
                    throw new RuntimeException(Logger.error(VulkanTexture.class, "Failed to submit per-RenderCommand command buffer"));
                }

                vkWaitForFences(VulkanRuntime.getCurrentDevice(), fence.getHandle(), true, VulkanUtil.UINT64_MAX);
            }
        }



    }

    private static TextureFormatType toTextureFormatType(int vulkanImageFormatEnum) {
        switch (vulkanImageFormatEnum) {
            case VK_FORMAT_R8G8B8A8_SRGB -> {
                return TextureFormatType.ColorR8G8B8A8StandardRGB;
            }
            case VK_FORMAT_D32_SFLOAT -> {
                return TextureFormatType.Depth32Float;
            }
        }

        return null;
    }

    public void transitionImageLayout(VkCommandBuffer commandBuffer, int oldLayout, int newLayout) {

        try(MemoryStack stack = stackPush()) {

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(oldLayout);
            barrier.newLayout(newLayout);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.image(this.image.getHandle());
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(1);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(1);

            int sourceStage;
            int destinationStage;

            if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {

                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;

            } else if(oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {

                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;

            } else {
                throw new IllegalArgumentException(Logger.error(VulkanTexture.class, "Unsupported Texture layout transition oldLayout(" + oldLayout + " newLayout(" + newLayout + ")"));
            }

            vkCmdPipelineBarrier(commandBuffer,
                    sourceStage, destinationStage,
                    0,
                    null,
                    null,
                    barrier);

        }
    }


    public VulkanImage getImage() {
        return image;
    }

    public VulkanImageView getImageView() {
        return imageView;
    }

    public VulkanSampler getSampler(){
        return sampler;
    }



    @Override
    public void dispose() {
        vkDeviceWaitIdle(VulkanRuntime.getCurrentDevice());
        vkDestroyCommandPool(VulkanRuntime.getCurrentDevice(), commandPool, null);

    }

}
