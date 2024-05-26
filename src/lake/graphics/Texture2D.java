package lake.graphics;

import lake.asset.Asset;
import lake.asset.TextureData;
import lake.graphics.vulkan.VulkanTexture2D;

public abstract class Texture2D implements Disposable {
    public int width, height;
    public Filter filter;
    public enum Filter {
        Linear,
        Nearest
    }

    public Texture2D(int width, int height){
        Disposer.add("managedResources", this);
        this.width = width;
        this.height = height;
    }

    public Texture2D(Asset<TextureData> textureData, Filter filter){
        Disposer.add("managedResources", this);
        this.width = textureData.asset.width;
        this.height = textureData.asset.height;
        this.filter = filter;
    }






    public abstract void setData(byte[] data);

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



    public static Texture2D newTexture2D(Asset<TextureData> textureData, Filter filter){
        if(Renderer2D.getRenderAPI() == RenderAPI.Vulkan) return new VulkanTexture2D(textureData, filter);

        return null;
    }

    public static Texture2D newTexture2D(int width, int height){
        if(Renderer2D.getRenderAPI() == RenderAPI.Vulkan) return new VulkanTexture2D(width, height);

        return null;
    }



}



