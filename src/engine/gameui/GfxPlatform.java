package engine.gameui;

import engine.graphics.Color;
import engine.graphics.text.MsdfFont;

public abstract class GfxPlatform {
    public abstract void drawRect(float x, float y, float w, float h, Color color);
    public abstract void drawString(float x, float y, String text, Color color);
}
