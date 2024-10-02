package fori.graphics;

import fori.asset.Asset;
import fori.asset.TextureData;
import fori.graphics.vulkan.VkTexture;

public abstract class Texture implements Disposable {
    private int width, height;
    private Filter minFilter, magFilter;
    private byte[] textureData;


    public enum Filter {
        Linear,
        Nearest
    }

    public byte[] getTextureData() {
        return textureData;
    }

    public Texture(int width, int height){
        Disposer.add("managedResources", this);
        this.width = width;
        this.height = height;
    }

    public Texture(Asset<TextureData> textureData, Filter minFilter, Filter magFilter){
        Disposer.add("managedResources", this);
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

    public static Texture newTexture(Asset<TextureData> textureData, Filter minFilter, Filter magFilter){
        if(SceneRenderer.getRenderAPI() == RenderAPI.Vulkan) return new VkTexture(textureData, minFilter, magFilter);
        return null;
    }

}



