package fori.ui;

import fori.graphics.Font;

public class WindowScope {
    public String title;
    public float x, y;
    public int id;
    public Font font;

    public WindowScope(String title, float x, float y, int id, Font font) {
        this.title = title;
        this.x = x;
        this.y = y;
        this.id = id;
        this.font = font;
    }
}
