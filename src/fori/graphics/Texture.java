package fori.graphics;

import fori.asset.Asset;
import fori.asset.TextureData;
import fori.graphics.vulkan.VulkanTexture;

public abstract class Texture implements Disposable {
    protected int width, height;
    protected Filter minFilter, magFilter;
    protected byte[] textureData;
    protected Ref ref;

    public enum Filter {
        Linear,
        Nearest
    }

    public byte[] getTextureData() {
        return textureData;
    }

    public Texture(Ref parent, Asset<TextureData> textureData, Filter minFilter, Filter magFilter){
        ref = parent.add(this);
        this.width = textureData.asset.width;
        this.height = textureData.asset.height;
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        this.textureData = textureData.asset.data;
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

    public static Texture newTexture(Ref parent, Asset<TextureData> textureData, Filter minFilter, Filter magFilter){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan) return new VulkanTexture(parent, textureData, minFilter, magFilter);
        return null;
    }

    @Override
    public Ref getRef() {
        return ref;
    }
}



