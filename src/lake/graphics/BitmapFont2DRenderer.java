package lake.graphics;

import java.util.Map;

public class BitmapFont2DRenderer {
    private BitmapFont2DRenderer(){

    }

    public static void drawText(float x, float y, String text, Color color, Font2D font, Renderer2D renderer2D) {

        Texture2D glyphTexture = font.getTexture();
        Map<Integer, Glyph> glyphs = font.getGlyphs();
        float xc = x;

        String line = "";

        for(char c : text.toCharArray()){
            Glyph glyph = glyphs.get((int) c);


            if(c == '\n'){
                y += font.getLineHeight(line);
                line = "";
                xc = x;
                continue;
            }


            float xt = glyph.getX();// + glyph.getxOffset();
            float yt = glyph.getY();// + glyph.getyOffset();

            float texX = xt / glyphTexture.getWidth();
            float texY = yt / glyphTexture.getHeight();

            float texW = (xt + glyph.getW()) / glyphTexture.getWidth();
            float texH = (yt + glyph.getH()) / glyphTexture.getHeight();

            renderer2D.drawTexture(xc + glyph.getxOffset(), y + (glyph.getyOffset()), glyph.getW(), glyph.getH(), glyphTexture, color, new Rect2D(texX, texY, texW, texH), false, false);


            xc += glyph.getXAdvance();

            line += c;
        }

    }


}
