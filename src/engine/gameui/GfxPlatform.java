package engine.gameui;

import engine.graphics.Color;
import engine.graphics.text.MsdfFont;
import engine.graphics.text.TextEffect;

public abstract class GfxPlatform {
    public abstract int getMouseX();
    public abstract int getMouseY();
    public abstract boolean isMousePressed(int mouseButton);
    public abstract void drawRect(float x, float y, float w, float h, Color color);
    public abstract void drawString(float x, float y, String text, MsdfFont font, TextEffect textEffect, Color color);
    public abstract Theme getTheme();
}
