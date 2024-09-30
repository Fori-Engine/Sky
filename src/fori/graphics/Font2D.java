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
public class Font2D {
    private Asset<TextureData> textureAsset;
    private Asset<String> fnt;
    private Texture texture;
    private HashMap<Integer, Glyph> glyphs = new HashMap<>();
    private float defaultLineHeight;



    public Font2D(Asset<TextureData> textureAsset, Asset<String> fnt) {
        this.textureAsset = textureAsset;
        this.fnt = fnt;

        texture = Texture.newTexture2D(textureAsset, Texture.Filter.Linear);
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
                Logger.todo(Font2D.class, "Attribute 'chars' not implemented in Font2D");
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


                glyphs.put(id, new Glyph(x, y, w, h, xo, yo, xAdvance));

            }

        }



    }

    public Texture getTexture() {
        return texture;
    }

    public HashMap<Integer, Glyph> getGlyphs() {
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

    private static Font2D defaultFont2D;

    public static Font2D getDefault(){




        if(defaultFont2D == null){
            defaultFont2D = new Font2D(
                    AssetPacks.getAsset("core:assets/fonts/arial/default.png"),
                    AssetPacks.getAsset("core:assets/fonts/arial/default.fnt")
            );
        }

        return defaultFont2D;
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

        for (String ignored : lines) {
            height += stringHeight(ignored);
        }

        return height;
    }

    private float stringWidth(String line){

        float x = 0;

        for (char c : line.toCharArray()){
            x += glyphs.get((int) c).getXAdvance();
        }

        return x;
    }

    private float stringHeight(String line){

        float h = 0;

        for (char c : line.toCharArray()){

            Glyph glyph = glyphs.get((int) c);

            float n = glyph.getH() + glyph.getyOffset();

            if(n > h){
                h = n;
            }
        }

        return h;
    }


    public float getWidthOf(String string){
        String[] lines = string.split("\n");
        float width = 0f;

        for(String line : lines){
            float lineWidth = stringWidth(line);

            if(lineWidth > width){
                width = lineWidth;
            }
        }

        return width;



    }

}
