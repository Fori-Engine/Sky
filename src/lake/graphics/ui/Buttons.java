package lake.graphics.ui;


public class Buttons {
    public static final int padding = 10;


    public static class ButtonEvent extends UIEvent {
        public boolean isPressed;

        public ButtonEvent(boolean isPressed) {
            this.isPressed = isPressed;
        }
    }


}
