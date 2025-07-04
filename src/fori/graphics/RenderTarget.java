package fori.graphics;

public class RenderTarget {
    private int textureCount;
    private Texture[] textures;

    public RenderTarget(int textureCount) {
        this.textureCount = textureCount;
        this.textures = new Texture[textureCount];
    }

    public void addTexture(int index, Texture texture) {
        textures[index] = texture;
    }

    public Texture getTexture(int index) {
        return textures[index];
    }
}
