package engine.gameui;

public class TextValue {
    public StringBuilder string = new StringBuilder();

    public TextValue(String string) {
        this.string.append(string);
    }

    public static TextValue text(String string) {
        return new TextValue(string);
    }
}
