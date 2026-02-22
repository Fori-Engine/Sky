package engine.graphics.text;

import engine.asset.Asset;
import engine.asset.AssetRegistry;
import engine.asset.TextureData;
import engine.graphics.Disposable;
import engine.graphics.Sampler;
import engine.graphics.Texture;
import engine.graphics.TextureFormatType;

public class MsdfFont extends Disposable {
    private Texture texture;
    private MsdfJsonLoader.MsdfData msdfData;
    private Sampler sampler;
    public MsdfFont(Disposable parent, Asset<TextureData> textureDataAsset, Asset<String> jsonAsset) {
        super(parent);

        texture = Texture.newColorTextureFromAsset(this, textureDataAsset, TextureFormatType.ColorR8G8B8A8);
        msdfData = MsdfJsonLoader.load(jsonAsset.getObject());
        sampler = Sampler.newSampler(texture, Texture.Filter.Linear, Texture.Filter.Linear, true);

    }

    public Texture getTexture() {
        return texture;
    }

    public MsdfJsonLoader.MsdfData getMSDFData() {
        return msdfData;
    }

    public Sampler getSampler() {
        return sampler;
    }

    @Override
    public void dispose() {}
}
