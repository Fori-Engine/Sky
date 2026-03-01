package engine.gameui;

import engine.graphics.Color;
import engine.graphics.text.MsdfFont;
;

public class ButtonWidget extends Widget {
    private String text;
    private Color color;
    private MsdfFont font;

    public ButtonWidget(String text, Color color, MsdfFont font) {
        this.text = text;
        this.color = color;
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
        platform.drawRect(x, y, w, h, color);
        platform.drawString(x, y, text, Color.WHITE);
        updateChildren(platform, x, y);
    }
}
