package fori.graphics.vulkan;

import fori.Logger;
import fori.asset.Asset;
import fori.asset.TextureData;
import fori.graphics.Buffer;
import fori.graphics.Ref;
import fori.graphics.Texture;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static fori.graphics.Texture.Filter.Linear;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VkTexture extends Texture {

    public VkImage image;
    public VkImageView imageView;
    public Buffer imageData;
    public long sampler;
    public boolean isCorrectLayout;


    public VkTexture(Ref parent, Asset<TextureData> textureData, Filter minFilter, Filter magFilter) {
        super(parent, textureData, minFilter, magFilter);

        image = new VkImage(
                ref,
                VkGlobalAllocator.getAllocator(),
                VkContextManager.getCurrentDevice(),
                getWidth(),
                getHeight(),
                VK_FORMAT_R8G8B8A8_SRGB ,
                VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                VK_IMAGE_TILING_OPTIMAL
        );



        imageData = Buffer.newBuffer(ref, getWidth() * getHeight() * 4, Buffer.Usage.ImageBackingBuffer, Buffer.Type.CPUGPUShared, false);
        ByteBuffer bytes = imageData.map();
        bytes.put(getTextureData());
        imageData.unmap();




        imageView = new VkImageView(image.getRef(), VkContextManager.getCurrentDevice(), image, VK_IMAGE_ASPECT_COLOR_BIT);

        try(MemoryStack stack = stackPush()){
            VkSamplerCreateInfo samplerCreateInfo = VkSamplerCreateInfo.calloc(stack);
            samplerCreateInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
            samplerCreateInfo.minFilter(minFilter == Linear ? VK_FILTER_LINEAR : VK_FILTER_NEAREST);
            samplerCreateInfo.magFilter(magFilter == Linear ? VK_FILTER_LINEAR : VK_FILTER_NEAREST);
            samplerCreateInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerCreateInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerCreateInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerCreateInfo.anisotropyEnable(true);
            samplerCreateInfo.maxAnisotropy(VkContextManager.getPhysicalDeviceProperties().limits().maxSamplerAnisotropy());
            samplerCreateInfo.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK);
            samplerCreateInfo.unnormalizedCoordinates(false);
            samplerCreateInfo.compareEnable(false);
            samplerCreateInfo.compareOp(VK_COMPARE_OP_ALWAYS);

            samplerCreateInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);

            LongBuffer pSampler = stack.callocLong(1);
            if(vkCreateSampler(VkContextManager.getCurrentDevice(), samplerCreateInfo, null, pSampler) != VK_SUCCESS){
                throw new RuntimeException(Logger.error(VkTexture.class, "Failed to create sampler!"));
            }

            sampler = pSampler.get(0);




        }



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
                throw new IllegalArgumentException(Logger.error(VkTexture.class, "Unsupported Texture layout transition oldLayout(" + oldLayout + " newLayout(" + newLayout + ")"));
            }

            vkCmdPipelineBarrier(commandBuffer,
                    sourceStage, destinationStage,
                    0,
                    null,
                    null,
                    barrier);

        }
    }




    public long getImageView() {
        return imageView.getHandle();
    }

    public long getSampler(){
        return sampler;
    }

    @Override
    public void dispose() {
        vkDeviceWaitIdle(VkContextManager.getCurrentDevice());

        vkDestroySampler(VkContextManager.getCurrentDevice(), sampler, null);



    }

}
