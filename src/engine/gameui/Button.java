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
        return (int) font.getStringWidth(value.string);
    }

    @Override
    public int getRequiredHeight() {
        return (int) font.getStringHeight(value.string);
    }

    @Override
    public void update(GfxPlatform platform, int x, int y, int w, int h) {
        if(Rect2D.contains(platform.getMouseX(), platform.getMouseY(), x, y, w, h)){
            platform.drawRect(x, y, w, h, Color.LIGHT_GRAY);

            boolean inputPressed = platform.isMousePressed(Input.MOUSE_BUTTON_1);
            if(pressed != inputPressed){

                if(pressed)
                    for(EventHandler eventHandler : getEventHandlers()) eventHandler.onClick();

                pressed = inputPressed;
            }

        }
        else platform.drawRect(x, y, w, h, Color.GRAY);

        platform.drawString(x, y, value.string, font, Color.WHITE);
        updateChildren(platform, x, y);
    }
}
