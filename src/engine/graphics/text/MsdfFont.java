package engine.graphics.text;

import engine.asset.Asset;
import engine.asset.TextureData;
import engine.graphics.Disposable;
import engine.graphics.Sampler;
import engine.graphics.Texture;
import engine.graphics.TextureFormatType;

public class MsdfFont extends Disposable {
    private Texture texture;
    private MsdfJsonLoader.MsdfData msdfData;
    private Sampler sampler;
    private int tabWidth = 4;
    public MsdfFont(Disposable parent, Asset<TextureData> textureDataAsset, Asset<String> jsonAsset) {
        super(parent);

        texture = Texture.newColorTextureFromAsset(this, textureDataAsset, TextureFormatType.ColorR8G8B8A8unorm);
        msdfData = MsdfJsonLoader.load(jsonAsset.getObject());
        sampler = Sampler.newSampler(texture, Texture.Filter.Linear, Texture.Filter.Linear, true);
    }

    public int getTabWidth() {
        return tabWidth;
    }

    public void setTabWidth(int tabWidth) {
        this.tabWidth = tabWidth;
    }

    public float getStringWidth(String string) {
        float spaceXAdvance = msdfData.characters[' '].advance;
        float width = 0;
        float stringLineWidth = 0;

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            if(c == '\n') {
                width = Math.max(stringLineWidth, width);
                stringLineWidth = 0;
                continue;
            }
            if(c == '\t') {
                stringLineWidth += tabWidth * spaceXAdvance;
                continue;
            }

            MsdfJsonLoader.Character character = msdfData.characters[c];
            if(character == null) character = msdfData.characters['?'];

            stringLineWidth += (character.advance) * msdfData.size;
        }
        width = Math.max(width, stringLineWidth);
        return (float) Math.ceil(width);
    }

    public float getStringHeight(String string) {
        float lineHeight = msdfData.lineHeight;
        int lineCount = 1;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if(c == '\n') lineCount++;
        }
        return lineCount * lineHeight * msdfData.size;
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
