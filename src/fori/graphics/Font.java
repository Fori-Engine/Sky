package fori.graphics;

import fori.Logger;
import fori.asset.Asset;
import fori.asset.AssetPacks;
import fori.asset.TextureData;

import java.util.HashMap;
import java.util.Map;

/***
 * Represents a Font that can be rendered to the screen. This class can also interoperate with AWT Fonts, as well as load TrueType (.ttf) fonts from disk.
 */
public class Font implements Disposable {
    private Asset<TextureData> textureAsset;
    private Asset<String> fnt;
    private Texture texture;
    private Glyph[] glyphs = new Glyph[1024];
    private float defaultLineHeight;
    private Ref ref;


    public Font(Ref parent, Asset<TextureData> textureAsset, Asset<String> fnt) {
        ref = parent.add(this);
        this.textureAsset = textureAsset;
        this.fnt = fnt;

        texture = Texture.newTexture(ref, textureAsset, Texture.Filter.Nearest, Texture.Filter.Nearest);
        createGlyphs();

    }




    public void createGlyphs(){
        String text = fnt.asset;

        for (String line : text.split("\n")){


            Map<String, String> parameters = getParameters(line);


            if(line.startsWith("common")){
                defaultLineHeight = Float.parseFloat(parameters.get("lineHeight"));
            }
            else if(line.startsWith("chars")){
                Logger.todo(Font.class, "Attribute 'chars' not implemented in Font2D");
            }
            else if(line.startsWith("char")){
                int id = Integer.parseInt(parameters.get("id"));

                float x = Float.parseFloat(parameters.get("x"));
                float y = Float.parseFloat(parameters.get("y"));
                float w = Float.parseFloat(parameters.get("width"));
                float h = Float.parseFloat(parameters.get("height"));

                float xo = Float.parseFloat(parameters.get("xoffset"));
                float yo = Float.parseFloat(parameters.get("yoffset"));

                float xAdvance = Float.parseFloat(parameters.get("xadvance"));


                glyphs[id] = new Glyph(x, y, w, h, xo, yo, xAdvance);

            }

        }



    }

    public Texture getTexture() {
        return texture;
    }

    public Glyph[] getGlyphs() {
        return glyphs;
    }

    public float getDefaultLineHeight() {
        return defaultLineHeight;
    }

    private Map<String, String> getParameters(String text) {

        HashMap<String, String> params = new HashMap<>();

        String[] split = text.split(" ");
        for (int i = 1; i < split.length; i++) {
            String kv = split[i];
            String[] tokens = kv.split("=");

            if(tokens.length > 1)
                params.put(tokens[0], tokens[1]);
        }



        return params;
    }

    public float getLineHeight(String line){

        float stringHeight = stringHeight(line);
        if(stringHeight == 0){
            stringHeight = getDefaultLineHeight();
        }

        return stringHeight;
    }


    public float getHeightOf(String string){
        String[] lines = string.split("\n");
        float height = 0f;

        for (String line : lines) {
            height += stringHeight(line);
        }

        return height;
    }

    private float stringWidth(String line){

        float x = 0;

        for (int i = 0; i < line.length(); i++){
            char c = line.charAt(i);

            x += glyphs[c].xadvance;
        }

        return x;
    }

    private float stringHeight(String line){

        float h = 0;

        for (int i = 0; i < line.length(); i++){
            char c = line.charAt(i);


            Glyph glyph = glyphs[c];

            float n = glyph.h + glyph.yo;

            if(n > h){
                h = n;
            }
        }

        return h;
    }


    public float getWidthOf(String string){

        float cw = 0f;
        float width = 0f;

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            if(c == '\n') {
                if(cw > width) {
                    width = cw;
                    cw = 0f;
                    continue;
                }
            }

            cw += glyphs[c].xadvance;

            if(i == string.length() - 1) width = cw;

        }

        return width;
    }

    @Override
    public void dispose() {

    }

    @Override
    public Ref getRef() {
        return null;
    }
}
