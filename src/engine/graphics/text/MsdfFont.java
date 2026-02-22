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
    public MsdfFont(Disposable parent, Asset<TextureData> textureDataAsset, Asset<String> jsonAsset) {
        super(parent);

        texture = Texture.newColorTextureFromAsset(this, textureDataAsset, TextureFormatType.ColorR8G8B8A8unorm);
        msdfData = MsdfJsonLoader.load(jsonAsset.getObject());
        sampler = Sampler.newSampler(texture, Texture.Filter.Linear, Texture.Filter.Linear, true);

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
                stringLineWidth += 4 * spaceXAdvance;
                continue;
            }

            MsdfJsonLoader.Character character = msdfData.characters[c];
            if(character == null) character = msdfData.characters['?'];

            stringLineWidth += character.advance * msdfData.size;
        }

        return width;
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

    public float getMaxHeightAboveBaselineOnFirstLine(String string) {

        float maxHeight = 0;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if(c == '\n') return maxHeight * msdfData.size;

            MsdfJsonLoader.Character character = msdfData.characters[c];
            if(character == null || character.planeBounds == null)
                continue;

            maxHeight = Math.max(maxHeight, character.planeBounds.top);
        }

        return -1;
    }
}
