package engine.ui;

public class ButtonEvent extends Event {
    public boolean buttonPressed, lock;

    public ButtonEvent(boolean buttonPressed) {
        this.buttonPressed = buttonPressed;
    }
}
