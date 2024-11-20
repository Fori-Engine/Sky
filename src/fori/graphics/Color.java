package fori.graphics;

public class Color {
    public float r, g, b, a;

    public static final Color RED = new Color(1f, 0f, 0f, 1f);
    public static final Color GREEN = new Color(0f, 1f, 0f, 1f);
    public static final Color BLUE = new Color(0f, 0f, 1f, 1f);
    public static final Color BLACK = new Color(0f, 0f, 0f, 1f);
    public static final Color WHITE = new Color(1f, 1f, 1f, 1f);
    public static final Color GRAY = new Color(0.5f, 0.5f, 0.5f, 1f);
    public static final Color LIGHT_GRAY = new Color(0.8f, 0.8f, 0.8f, 1f);

    public Color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public Color(int hexcode) {
        this(
                ((hexcode >> 16) & 0xFF) / 255f,
                ((hexcode >> 8) & 0xFF) / 255f,
                (hexcode & 0xFF) / 255f,
                1.0f
        );
    }

    public static Color fromRGB(float r, float g, float b, float a){
        float scale = 1 / 255f;
        return new Color(scale * r, scale * g, scale * b, scale * a);
    }

    public Color mul(float s){
        return new Color(s * r, s * g, s * b, s * a);
    }

    @Override
    public String toString() {
        return "Color [" + r + " " + g + " " + b + " " + a + "]";
    }
}
