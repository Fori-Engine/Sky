package fori.graphics;

import fori.asset.Asset;
import fori.asset.TextureData;
import fori.graphics.vulkan.VulkanTexture;

public abstract class Texture extends Disposable {
    protected int width, height;
    protected Filter minFilter, magFilter;
    protected byte[] textureData;

    public enum Filter {
        Linear,
        Nearest
    }

    public byte[] getTextureData() {
        return textureData;
    }

    public Texture(Disposable parent, int width, int height, Asset<TextureData> textureData, Filter minFilter, Filter magFilter){
        super(parent);
        if(textureData != null) {
            this.textureData = textureData.asset.data;
        }
        this.width = width;
        this.height = height;
        this.minFilter = minFilter;
        this.magFilter = magFilter;

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

    public static Texture newTexture(Disposable parent, Asset<TextureData> textureData, Filter minFilter, Filter magFilter){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan) return new VulkanTexture(parent, textureData.asset.width, textureData.asset.height, textureData, minFilter, magFilter);
        return null;
    }

    public static Texture newTexture(Disposable parent, int width, int height, Filter minFilter, Filter magFilter){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan) return new VulkanTexture(parent, width, height, null, minFilter, magFilter);
        return null;
    }

}



