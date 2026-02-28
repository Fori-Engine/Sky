package engine.ui;

import engine.graphics.text.MsdfFont;

public class WindowScope {

    public String title;
    public float x, y;
    public String id;
    public MsdfFont font;

    public WindowScope(String title, float x, float y, String id, MsdfFont font) {
        this.title = title;
        this.x = x;
        this.y = y;
        this.id = id;
        this.font = font;
    }


}
