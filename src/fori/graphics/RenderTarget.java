package fori.graphics;

public class RenderTarget extends Disposable {
    private Texture[] textures;

    public RenderTarget(Disposable parent, int textureCount) {
        super(parent);
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

    @Override
    public void dispose() {

    }
}
