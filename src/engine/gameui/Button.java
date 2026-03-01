package engine.gameui;

import engine.Input;
import engine.graphics.Color;
import engine.graphics.Rect2D;
import engine.graphics.text.MsdfFont;
;

public class Button extends Widget {
    private String text;
    private MsdfFont font;
    private boolean pressed;

    public Button(String text, MsdfFont font) {
        this.text = text;
        this.font = font;
    }

    @Override
    public int getRequiredWidth() {
        return (int) font.getStringWidth(text);
    }

    @Override
    public int getRequiredHeight() {
        return (int) font.getStringHeight(text);
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

        platform.drawString(x, y, text, font, Color.WHITE);
        updateChildren(platform, x, y);
    }
}
