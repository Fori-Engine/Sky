package engine.ui;

import engine.Surface;
import engine.graphics.Rect2D;

public class WindowEvent extends Event {
    public String title;
    public float x = 0, y = 0;
    public float sx = 0, sy = 0;
    public boolean initialSelect;
    public Rect2D windowRect;
    public Surface.Cursor cursor;

    public WindowEvent(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
