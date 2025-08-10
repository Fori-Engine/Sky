package fori.graphics;

public class RenderTargetAttachment {
    private long mask = 0;
    private Texture[] textures;

    public RenderTargetAttachment(long mask, Texture[] textures) {
        this.mask = mask;
        this.textures = textures;
    }

    public long getMask() {
        return mask;
    }

    public Texture[] getTextures() {
        return textures;
    }
}
