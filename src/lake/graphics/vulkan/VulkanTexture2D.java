package lake.graphics.vulkan;

import lake.asset.Asset;
import lake.asset.TextureData;
import lake.graphics.Texture2D;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanTexture2D extends Texture2D {


    private VulkanBuffer stagingBuffer;
    private long stagingBufferMemory;
    private LongBuffer pTextureImage;
    private LongBuffer pTextureImageMemory;
    private VkDevice device;
    private long textureImage, textureImageMemory;
    private long textureImageView;
    private PointerBuffer data;
    private VulkanSampler2D sampler;
    private boolean isInitialized;


    public VulkanTexture2D(int width, int height){
        super(width, height);

        device = VulkanRenderer2D.getDeviceWithIndices().device;
        isInitialized = true;
        createResources(width * height * 4);
        createTextureResources(width, height, 1, VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT);
    }

    public VulkanTexture2D(Asset<TextureData> textureData, Filter filter) {
        super(textureData, filter);

        device = VulkanRenderer2D.getDeviceWithIndices().device;
        isInitialized = true;
        createResources(textureData.asset.data.length);


        stagingBuffer.mapAndUpload(device, data, textureData.asset.data);
        createTextureResources(width, height, 1, VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT);
        copyBufferToImage();
    }

    public void createResources(int sizeBytes){

        int minFilter = filter == Filter.Linear ? VK_FILTER_LINEAR : VK_FILTER_NEAREST;
        int magFilter = filter == Filter.Linear ? VK_FILTER_LINEAR : VK_FILTER_NEAREST;

        sampler = new VulkanSampler2D(device, minFilter, magFilter);


        LongBuffer pStagingBufferMemory = MemoryUtil.memAllocLong(1);
        stagingBuffer = FastVK.createBuffer(
                device,
                VulkanRenderer2D.getPhysicalDevice(),
                sizeBytes,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                pStagingBufferMemory
        );
        stagingBufferMemory = pStagingBufferMemory.get(0);

        data = MemoryUtil.memAllocPointer(1);
    }


    public void createTextureResources(int width, int height, int samples, int usage){

        VkPhysicalDevice physicalDevice = VulkanRenderer2D.getPhysicalDevice();
        try(MemoryStack stack = stackPush()) {

            pTextureImage = stack.mallocLong(1);
            pTextureImageMemory = stack.mallocLong(1);


            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
            imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(width);
            imageInfo.extent().height(height);
            imageInfo.extent().depth(1);
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(1);
            imageInfo.format(VK_FORMAT_R8G8B8A8_SRGB);
            imageInfo.tiling(VK_IMAGE_TILING_OPTIMAL);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(usage);
            imageInfo.samples(samples);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            if(vkCreateImage(device, imageInfo, null, pTextureImage) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create image");
            }

            textureImage = pTextureImage.get(0);



            VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device, textureImage, memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            allocInfo.memoryTypeIndex(FastVK.findMemoryType(memRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, physicalDevice));



            if(vkAllocateMemory(device, allocInfo, null, pTextureImageMemory) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate image memory");
            }

            textureImageMemory = pTextureImageMemory.get(0);

            vkBindImageMemory(device, textureImage, textureImageMemory, 0);





        }

    }

    public void copyBufferToImage(){

        try(MemoryStack stack = stackPush()) {

            FastVK.run(device, stack, (commandBuffer) -> {

                transition(textureImage, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, commandBuffer, stack);
                //Buffer -> Image
                {
                    VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
                    region.bufferOffset(0);
                    region.bufferRowLength(0);   // Tightly packed
                    region.bufferImageHeight(0);  // Tightly packed
                    region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    region.imageSubresource().mipLevel(0);
                    region.imageSubresource().baseArrayLayer(0);
                    region.imageSubresource().layerCount(1);
                    region.imageOffset().set(0, 0, 0);
                    region.imageExtent(VkExtent3D.calloc(stack).set(getWidth(), getHeight(), 1));

                    vkCmdCopyBufferToImage(commandBuffer, stagingBuffer.handle, textureImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
                }
                transition(textureImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, commandBuffer, stack);


                int format = VK_FORMAT_R8G8B8A8_SRGB;


                //Image View
                {

                    VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
                    viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
                    viewInfo.image(textureImage);
                    viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
                    viewInfo.format(format);
                    viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    viewInfo.subresourceRange().baseMipLevel(0);
                    viewInfo.subresourceRange().levelCount(1);
                    viewInfo.subresourceRange().baseArrayLayer(0);
                    viewInfo.subresourceRange().layerCount(1);

                    LongBuffer pImageView = stack.mallocLong(1);

                    if (vkCreateImageView(device, viewInfo, null, pImageView) != VK_SUCCESS) {
                        throw new RuntimeException("Failed to create texture image view");
                    }
                    textureImageView = pImageView.get(0);

                }


            });

        }
    }

    public long getTextureImage() {
        return textureImage;
    }

    public long getTextureImageView() {
        return textureImageView;
    }

    private void transition(long textureImage, int oldLayout, int newLayout, VkCommandBuffer commandBuffer, MemoryStack stack){


        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.oldLayout(oldLayout);
        barrier.newLayout(newLayout);
        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.image(textureImage);
        barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        barrier.subresourceRange().baseMipLevel(0);
        barrier.subresourceRange().levelCount(1);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(1);

        int sourceStage;
        int destinationStage;

        if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {

            barrier.srcAccessMask(0);
            barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

            sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;

        } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {

            barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

            sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;

        } else {
            throw new IllegalArgumentException("Unsupported layout transition");
        }

        vkCmdPipelineBarrier(commandBuffer,
                sourceStage, destinationStage,
                0,
                null,
                null,
                barrier);
    }



    @Override
    public void setData(byte[] data) {

    }

    public VulkanSampler2D getSampler() {
        return sampler;
    }

    @Override
    public void dispose() {

        if(isInitialized) {

            vkDestroyImageView(device, textureImageView, null);
            vkDestroyImage(device, textureImage, null);
            vkDestroyBuffer(device, stagingBuffer.handle, null);
            vkFreeMemory(device, stagingBufferMemory, null);
            vkFreeMemory(device, textureImageMemory, null);
        }
    }

}
