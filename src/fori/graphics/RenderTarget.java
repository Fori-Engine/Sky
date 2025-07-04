package fori.graphics;

public class RenderTarget {
    private Texture[] textures;

    public RenderTarget(int textureCount) {
        this.textures = new Texture[textureCount];
    }

    public void addTexture(int index, Texture texture) {
        textures[index] = texture;
    }

    public Texture getTexture(int index) {
        return textures[index];
    }

    public int getTextureCount() {
        return textures.length;
    }
}
