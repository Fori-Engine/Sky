package engine.gameui;

import engine.graphics.Color;
import engine.graphics.text.MsdfFont;

;

public class Text extends Widget {
    private TextValue value;
    private MsdfFont font;

    public Text(TextValue value, MsdfFont font) {
        this.value = value;
        this.font = font;
    }

    @Override
    public int getRequiredWidth() {
        return (int) font.getStringWidth(value.string);
    }

    public TextValue getText() {
        return value;
    }

    @Override
    public int getRequiredHeight() {
        return (int) font.getStringHeight(value.string);
    }

    @Override
    public void update(GfxPlatform platform, int x, int y, int w, int h) {

        platform.drawString(x, y, value.string, font, null, Color.WHITE);
        updateChildren(platform, x, y, w, h);
    }
}
