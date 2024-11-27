package fori.ui;

import fori.graphics.Color;
import fori.graphics.Font;
import fori.graphics.Texture;
import org.joml.Vector2f;

public abstract class Adapter {
    public abstract Vector2f getSize();
    public abstract void drawFilledRect(float x, float y, float w, float h, Color color);
    public abstract void drawRect(float x, float y, float w, float h, Color color);
    public abstract void drawText(float x, float y, String text, Font font, Color color);
    public abstract void drawTexture(float x, float y, float w, float h, float tx, float ty, float tw, float th, Texture texture, Color color);
    public abstract void drawFilledCircle(float x, float y, float w, float h, float thickness, Color color);
}
