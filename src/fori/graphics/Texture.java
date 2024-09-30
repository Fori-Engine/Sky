package fori.graphics;

import fori.asset.Asset;
import fori.asset.TextureData;

public abstract class Texture implements Disposable {
    public int width, height;
    public Filter filter;

    public Texture() {

    }

    public enum Filter {
        Linear,
        Nearest
    }

    public enum Tiling {
        Linear,
        Optimal
    }

    public Texture(int width, int height){
        Disposer.add("managedResources", this);
        this.width = width;
        this.height = height;
    }

    public Texture(Asset<TextureData> textureData, Filter filter){
        Disposer.add("managedResources", this);
        this.width = textureData.asset.width;
        this.height = textureData.asset.height;
        this.filter = filter;
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



    public static Texture newTexture2D(Asset<TextureData> textureData, Filter filter){

        return null;
    }

    public static Texture newTexture2D(int width, int height){

        return null;
    }



}



