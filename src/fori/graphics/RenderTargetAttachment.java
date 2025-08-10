package fori.graphics;

public class RenderTargetAttachment {
    private long flags = 0;
    private Texture[] textures;

    public RenderTargetAttachment(long flags, Texture[] textures) {
        this.flags = flags;
        this.textures = textures;
    }

    public long getFlags() {
        return flags;
    }

    public Texture[] getTextures() {
        return textures;
    }
}
