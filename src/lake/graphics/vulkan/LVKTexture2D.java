package lake.graphics.vulkan;

import lake.graphics.Disposer;
import lake.graphics.Texture2D;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static lake.graphics.vulkan.FastVK.findMemoryType;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class LVKTexture2D extends Texture2D {


    private LVKGenericBuffer stagingBuffer;
    private long stagingBufferMemory;
    private LongBuffer pTextureImage;
    private LongBuffer pTextureImageMemory;

    public LVKTexture2D(String path){
        this(path, Filter.LINEAR);
    }

    public LVKTexture2D(int width, int height) {
        Disposer.add("managedResources", this);
        setProperties(null, width, height);

        //TODO: Wth does this do?
    }
    public LVKTexture2D(String path, Texture2D.Filter filter) {
        Disposer.add("managedResources", this);

        VkDevice device = LVKRenderer2D.getDeviceWithIndices().device;
        VkPhysicalDevice physicalDevice = LVKRenderer2D.getPhysicalDevice();

        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer channelsInFile = BufferUtils.createIntBuffer(1);
        ByteBuffer texture = STBImage.stbi_load(path, w, h, channelsInFile, 4);
        System.out.println(STBImage.stbi_failure_reason());
        int width = w.get();
        int height = h.get();

        setProperties(path, width, height);

        LongBuffer pStagingBufferMemory = MemoryUtil.memAllocLong(1);
        stagingBuffer = FastVK.createBuffer(
                device,
                LVKRenderer2D.getPhysicalDevice(),
                width * height * 4,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                pStagingBufferMemory
        );
        stagingBufferMemory = pStagingBufferMemory.get(0);

        PointerBuffer data = MemoryUtil.memAllocPointer(1);

        byte[] bytes = new byte[texture.remaining()];
        texture.get(bytes);

        stagingBuffer.mapAndUpload(device, data, bytes);






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
            imageInfo.usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            if(vkCreateImage(device, imageInfo, null, pTextureImage) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create image");
            }

            VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device, pTextureImage.get(0), memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            allocInfo.memoryTypeIndex(FastVK.findMemoryType(memRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, physicalDevice));



            if(vkAllocateMemory(device, allocInfo, null, pTextureImageMemory) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate image memory");
            }

            vkBindImageMemory(device, pTextureImage.get(0), pTextureImageMemory.get(0), 0);
        }






























        STBImage.stbi_image_free(texture);
    }




    @Override
    public void dispose() {

    }

    @Override
    public void setData(ByteBuffer data, int width, int height) {

    }
}
