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
    private VulkanCommandPool commandPool;


    public VulkanTexture(Disposable parent, int width, int height, long imageHandle, int currentLayout, int usage, int imageFormat, int aspectMask) {
        super(parent, width, height, null, toTextureFormatType(imageFormat), Nearest, Nearest);
        image = new VulkanImage(
                this,
                imageHandle,
                currentLayout,
                usage,
                imageFormat
        );
        imageView = new VulkanImageView(image, VulkanRuntime.getCurrentDevice(), image, aspectMask);
        sampler = new VulkanSampler(this, minFilter, magFilter, false);
        isStorageTexture = (usage & VK_IMAGE_USAGE_STORAGE_BIT) != 0;
    }

    public VulkanTexture(Disposable parent, int width, int height, Asset<TextureData> textureData, Filter minFilter, Filter magFilter, int imageFormat, int usage, int tiling, int aspectMask) {
        super(parent, width, height, textureData, toTextureFormatType(imageFormat), minFilter, magFilter);

        image = new VulkanImage(
                this,
                VulkanAllocator.getAllocator(),
                getWidth(),
                getHeight(),
                imageFormat,
                usage,
                tiling
        );

        isStorageTexture = (usage & VK_IMAGE_USAGE_STORAGE_BIT) != 0;

        imageView = new VulkanImageView(image, VulkanRuntime.getCurrentDevice(), image, aspectMask);
        sampler = new VulkanSampler(this, minFilter, magFilter, false);

        if(textureData != null) {


            imageData = Buffer.newBuffer(this, getWidth() * getHeight() * 4, Buffer.Usage.ImageBackingBuffer, Buffer.Type.CPUGPUShared, false);
            ByteBuffer bytes = imageData.map();
            bytes.put(getTextureData());
            imageData.unmap();






            try (MemoryStack stack = stackPush()) {


                commandPool = new VulkanCommandPool(this, VulkanRuntime.getCurrentDevice(), VulkanRuntime.getGraphicsFamilyIndex());

                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(commandPool.getHandle());
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


                VulkanUtil.transitionImageLayout(
                        image,
                        commandBuffer,
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        0,
                        VK_ACCESS_TRANSFER_WRITE_BIT,
                        VK_IMAGE_ASPECT_COLOR_BIT,
                        VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                        VK_PIPELINE_STAGE_TRANSFER_BIT
                );



                VkBufferImageCopy.Buffer imageCopies = VkBufferImageCopy.calloc(1, stack);
                imageCopies.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                imageCopies.imageSubresource().layerCount(1);
                imageCopies.imageExtent().set(width, height, 1);


                vkCmdCopyBufferToImage(commandBuffer, ((VulkanBuffer) imageData).getHandle(), image.getHandle(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, imageCopies);


                VulkanUtil.transitionImageLayout(
                        image,
                        commandBuffer,
                        isStorageTexture ? VK_IMAGE_LAYOUT_GENERAL : VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                        VK_ACCESS_TRANSFER_WRITE_BIT,
                        VK_ACCESS_SHADER_READ_BIT,
                        VK_IMAGE_ASPECT_COLOR_BIT,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
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
                return TextureFormatType.ColorR8G8B8A8;
            }
            case VK_FORMAT_R32G32B32A32_SFLOAT -> {
                return TextureFormatType.ColorR32G32B32A32;
            }
            case VK_FORMAT_D32_SFLOAT -> {
                return TextureFormatType.Depth32;
            }
        }

        return null;
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
    public void dispose() {}

}
