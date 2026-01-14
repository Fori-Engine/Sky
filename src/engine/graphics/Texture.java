package engine.graphics;

import engine.asset.Asset;
import engine.asset.TextureData;
import engine.graphics.vulkan.VulkanTexture;
import engine.graphics.vulkan.VulkanUtil;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_DEPTH_BIT;

public abstract class Texture extends Disposable {
    protected int width, height;
    protected byte[] textureData;
    protected TextureFormatType formatType;
    protected boolean isStorageTexture;

    public enum Filter {
        Linear,
        Nearest
    }

    public byte[] getTextureData() {
        return textureData;
    }

    public Texture(Disposable parent, int width, int height, Asset<TextureData> textureData, TextureFormatType formatType){
        super(parent);
        if(textureData != null) {
            this.textureData = textureData.getObject().data;
        }
        this.width = width;
        this.height = height;
        this.formatType = formatType;
    }

    public TextureFormatType getFormatType() {
        return formatType;
    }

    public boolean isStorageTexture() {
        return isStorageTexture;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }


    public static Texture newColorTextureFromAsset(Disposable parent, Asset<TextureData> textureData, TextureFormatType textureFormatType){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan) {
            return new VulkanTexture(
                    parent,
                    textureData.getObject().width,
                    textureData.getObject().height,
                    textureData,
                    VulkanUtil.getVulkanImageFormat(textureFormatType),
                    VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_ASPECT_COLOR_BIT
            );
        }
        return null;
    }

    public static Texture newColorTexture(Disposable parent, int width, int height, TextureFormatType textureFormatType){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan) {
            return new VulkanTexture(
                    parent,
                    width,
                    height,
                    null,
                    VulkanUtil.getVulkanImageFormat(textureFormatType),
                    VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_ASPECT_COLOR_BIT
            );
        }
        return null;
    }

    public static Texture newStorageTexture(Disposable parent, int width, int height, TextureFormatType textureFormatType){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan) {
            return new VulkanTexture(
                    parent,
                    width,
                    height,
                    null,
                    VulkanUtil.getVulkanImageFormat(textureFormatType),
                    VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_ASPECT_COLOR_BIT
            );
        }
        return null;
    }

    public static Texture newDepthTexture(Disposable parent, int width, int height, TextureFormatType textureFormatType){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan) {
            return new VulkanTexture(
                    parent,
                    width,
                    height,
                    null,
                    VulkanUtil.getVulkanImageFormat(textureFormatType),
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_ASPECT_DEPTH_BIT
            );
        }
        return null;
    }

}



