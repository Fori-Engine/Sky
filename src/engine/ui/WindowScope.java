package engine.ui;

import engine.graphics.Font;

public class WindowScope {
    public String title;
    public float x, y;
    public String id;
    public Font font;

    public WindowScope(String title, float x, float y, String id, Font font) {
        this.title = title;
        this.x = x;
        this.y = y;
        this.id = id;
        this.font = font;
    }
}
