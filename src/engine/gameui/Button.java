package engine.gameui;

import engine.Input;
import engine.graphics.Color;
import engine.graphics.Rect2D;
import engine.graphics.text.MsdfFont;

public class Button extends Widget {
    private TextValue value;
    private MsdfFont font;
    private boolean pressed;

    public Button(TextValue value, MsdfFont font) {
        this.value = value;
        this.font = font;
    }

    @Override
    public int getRequiredWidth() {
        return (int) font.getStringWidth(value.string) + (6 * padding);
    }

    @Override
    public int getRequiredHeight() {
        return (int) font.getStringHeight(value.string) + (2 * padding);
    }

    @Override
    public void update(GfxPlatform platform, int x, int y, int w, int h) {
        if(Rect2D.contains(platform.getMouseX(), platform.getMouseY(), x, y, w, h)){
            platform.drawRect(x, y, w, h, platform.getTheme().buttonHoverColor);

            boolean inputPressed = platform.isMousePressed(Input.MOUSE_BUTTON_1);
            if(pressed) {
                platform.drawRect(x, y, w, h, platform.getTheme().buttonClickColor);
            }
            else {
                platform.drawRect(x, y, w, h, platform.getTheme().buttonHoverColor);
            }


            if(pressed != inputPressed){

                if(pressed)
                    for(EventHandler eventHandler : getEventHandlers()) eventHandler.onClick();

                pressed = inputPressed;
            }

        }
        else
            platform.drawRect(x, y, w, h, platform.getTheme().buttonBackgroundColor);

        platform.drawString(x + (3 * padding), y + padding, value.string, font, null, Color.WHITE);
        updateChildren(platform, x + (3 * padding), y + padding, w - padding, h - padding);
    }
}
