package engine.gameui;

import engine.Input;
import engine.SurfaceCharCallback;
import engine.SurfaceKeyCallback;
import engine.graphics.Color;
import engine.graphics.Rect2D;
import engine.graphics.Session;
import engine.graphics.text.MsdfFont;

;

public class TextField extends Widget {
    private TextValue value;
    private MsdfFont font;
    private SurfaceCharCallback surfaceCharCallback;
    private SurfaceKeyCallback surfaceKeyCallback;
    private int columns = 15;


    public TextField(TextValue value, MsdfFont font) {
        this.value = value;
        this.font = font;
        surfaceKeyCallback = key -> {
            if(focused) {
                if (key == Input.KEY_BACKSPACE) {
                    if (value.string.isEmpty()) return;
                    value.string.deleteCharAt(value.string.length() - 1);
                }
            }
        };
        surfaceCharCallback = new SurfaceCharCallback() {
            @Override
            public void keyClick(char c) {
                if(focused) value.string.append(c);
            }
        };

        Session.getSurface().addKeyCallback(surfaceKeyCallback);
        Session.getSurface().addCharCallback(surfaceCharCallback);
    }

    public int getColumns() {
        return columns;
    }

    public TextField setColumns(int columns) {
        this.columns = columns;
        return this;
    }

    private int firstWidth = 0;

    private String getVisibleString() {
        if(value.string.isEmpty()) return "";
        if(value.string.length() <= columns) return value.string.toString();

        String string = value.string.substring(value.string.length() - columns, value.string.length());
        int newWidth = (int) font.getStringWidth(string) + (2 * padding);
        if(firstWidth == 0 || newWidth > firstWidth)
            firstWidth = (int) font.getStringWidth(string) + (2 * padding);

        return string;
    }

    @Override
    public int getRequiredWidth() {
        return firstWidth == 0 ? (int) font.getStringWidth(getVisibleString()) + (2 * padding) : firstWidth + (2 * padding);
    }

    public TextValue getText() {
        return value;
    }

    @Override
    public int getRequiredHeight() {
        return (int) font.getStringHeight(value.string.toString()) + (2 * padding);
    }

    @Override
    public void update(GfxPlatform platform, int x, int y, int w, int h) {

        if(platform.isMousePressed(Input.MOUSE_BUTTON_1)) {
            focused = Rect2D.contains(platform.getMouseX(), platform.getMouseY(), x + padding, y + padding, w - padding * 2, h - padding * 2);
        }

        Color color = focused ? platform.getTheme().buttonHoverColor : platform.getTheme().buttonBackgroundColor;

        platform.drawString(
                x + padding,
                y + padding,
                getVisibleString(),
                font,
                null,
                Color.WHITE
        );
        platform.drawRectLines(x + padding, y + padding, w - padding * 2, h - padding * 2, 2, color);
        updateChildren(platform, x + padding, y + padding, w - padding * 2, h - padding * 2);
    }
}
