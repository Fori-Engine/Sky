package engine.ui;

import engine.graphics.Color;

public class DarkMode extends Theme {
    public DarkMode() {
        panelBackground = new Color(0x5e5e5e);
        windowBackground = new Color(0x383838);
        windowHeaderBackground = new Color(0x252525);
        buttonIdleBackground = new Color(0x4b4b4b);
        buttonHoverBackground = new Color(0x5e5e5e);
        buttonClickBackground = new Color(0x707070);
        buttonForeground = Color.WHITE;
        textForeground = Color.WHITE;

        windowPadding = 2f;
        windowHeaderPadding = 6f;
        buttonPadding = 4f;
        panelPadding = 3f;
        textPadding = 6f;
        windowShadowCount = 1;



    }
}
