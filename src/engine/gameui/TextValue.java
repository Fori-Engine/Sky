package engine.gameui;

public class TextValue {
    public String string;

    public TextValue(String string) {
        this.string = string;
    }

    public static TextValue text(String string) {
        return new TextValue(string);
    }
}
