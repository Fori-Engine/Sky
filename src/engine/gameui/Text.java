package engine.gameui;

import engine.Input;
import engine.graphics.Color;
import engine.graphics.Rect2D;
import engine.graphics.text.MsdfFont;

;

public class Text extends Widget {
    private String text;
    private MsdfFont font;

    public Text(String text, MsdfFont font) {
        this.text = text;
        this.font = font;
    }

    @Override
    public int getRequiredWidth() {
        return (int) font.getStringWidth(text);
    }

    @Override
    public int getRequiredHeight() {
        return (int) font.getStringHeight(text);
    }

    @Override
    public void update(GfxPlatform platform, int x, int y, int w, int h) {

        platform.drawString(x, y, text, font, Color.WHITE);
        updateChildren(platform, x, y);
    }
}
