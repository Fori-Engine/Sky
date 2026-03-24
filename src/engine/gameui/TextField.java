package engine.gameui;

import engine.Input;
import engine.SurfaceCharCallback;
import engine.SurfaceKeyCallback;
import engine.Time;
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
    private int cursor = 0;
    private int determinedWidth = 0;

    public TextField(TextValue value, MsdfFont font) {
        this.value = value;
        this.font = font;
        surfaceKeyCallback = (key, modifiers) -> {
            if(focused) {
                if (key == Input.KEY_BACKSPACE) {
                    if (value.string.isEmpty() || cursor == 0) return;
                    value.string.deleteCharAt(cursor - 1);
                    cursor--;
                }

                if (key == Input.KEY_LEFT) if (cursor > 0) cursor--;
                if (key == Input.KEY_RIGHT) if (cursor < value.string.length()) cursor++;


                if(key == Input.KEY_V && (modifiers & Input.MOD_CONTROL) != 0) {
                    value.string.append(Session.getSurface().getClipboardString());
                }
            }
        };
        surfaceCharCallback = new SurfaceCharCallback() {
            @Override
            public void keyClick(char c) {
                if(focused) {
                    value.string.insert(cursor, c);
                    cursor++;
                }
            }
        };

        determinedWidth = (int) (font.getStringWidth(value.string.toString()) + (padding * 2));
        columns = value.string.length();
        cursor = columns;


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


    private String getVisibleString() {
        return value.string.toString().substring(cursor, Math.min(cursor + columns, value.string.length()));
    }

    @Override
    public int getRequiredWidth() {
        return determinedWidth;
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

        String visibleString = getVisibleString();
        platform.drawString(
                x + padding,
                y + padding,
                visibleString,
                font,
                null,
                platform.getTheme().textColor
        );

        Color color = focused ? platform.getTheme().buttonHoverColor : platform.getTheme().buttonBackgroundColor;

        platform.drawRectLines(x + padding, y + padding, w - padding * 2, h - padding * 2, 2, color);
        updateChildren(platform, x + padding, y + padding, w - padding * 2, h - padding * 2);
    }
}
